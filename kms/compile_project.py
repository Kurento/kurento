#!/usr/bin/python2.7 -u
#pylint: disable=missing-docstring

# Module documentation
# - argparse: https://docs.python.org/2/library/argparse.html
# - python-apt: https://apt.alioth.debian.org/python-apt-doc/contents.html
# - python-debian: source code
# - python-requests: http://docs.python-requests.org/en/master/api/

from __future__ import print_function

import argparse
import glob
import io
import multiprocessing
import os
import re
import subprocess
from datetime import datetime
from time import strftime, time

import apt.cache
import apt.debfile
import apt_pkg
from debian import changelog
from debian import deb822
import git
import requests
import yaml


DEFAULT_CONFIG_FILE = '.build.yaml'
APT_CACHE = apt.cache.Cache()


def root_privileges_drop():
    if (os.geteuid() == 0
            and 'SUDO_GID' in os.environ
            and 'SUDO_UID' in os.environ):
        sudo_gid = int(os.environ['SUDO_GID'])
        sudo_uid = int(os.environ['SUDO_UID'])
        os.setresgid(sudo_gid, sudo_gid, -1)
        os.setresuid(sudo_uid, sudo_uid, -1)


def root_privileges_gain():
    if os.geteuid() != 0:
        os.setresgid(0, 0, -1)
        os.setresuid(0, 0, -1)


def apt_cache_update():
    global APT_CACHE
    root_privileges_gain()
    try:
        APT_CACHE.update()
    except IOError as err:
        print("[buildpkg::apt_cache_update] ERROR:", err)
        exit(1)
    APT_CACHE.open()
    root_privileges_drop()


def apt_cache_commit():
    global APT_CACHE
    root_privileges_gain()
    try:
        APT_CACHE.commit()
    except SystemError as err:
        print("[buildpkg::apt_cache_commit] ERROR:", err)
        exit(1)
    APT_CACHE.open()
    root_privileges_drop()


# Input of this function is a single dependency, which is a list of alternatives
def dep2str(dep_alts):
    if isinstance(dep_alts[0], dict):
        return deb822.PkgRelation.str([dep_alts])
    if isinstance(dep_alts[0], list):
        return deb822.PkgRelation.str(dep_alts)
    return ""


def clone_repo(base_url, repo_name):
    try:
        repo = git.Repo(repo_name)
    except git.NoSuchPathError:
        repo_url = base_url + "/" + repo_name
        print("[buildpkg::clone_repo] Clone URL:", repo_url)
        repo = git.Repo.clone_from(repo_url, repo_name)
    else:
        # TODO: Decide if current branch should be updated
        print("[buildpkg::clone_repo] Update repo:", repo_name)
        for remote in repo.remotes:
            remote.update()
    return repo


def get_pkgversion_to_install(pkg, req_version, req_commit):
    def check_req_version(version_str, req_version):
        print("[buildpkg::get_pkgversion_to_install] Test '{} {} {}'".format(
            version_str, req_version[0], req_version[1]))
        return apt_pkg.check_dep(version_str, req_version[0], req_version[1])

    pkgversion_found = None

    # If a specific commit is required, enforce that.
    # Else if a version is required, try that one.
    # Else upon no restrictions on version or commit, use the installed one
    # if any, or install the current candidate.
    if req_commit:
        for pkgversion in pkg.versions:
            if pkgversion.version.find(req_commit[:7]) >= 0:
                pkgversion_found = pkgversion
                break
    elif req_version:
        # First check the installed version, if any.
        # As a second option, resort to the current candidate.
        # If that still doesn't work, review all other versions available.
        if pkg.is_installed and check_req_version(pkg.installed.version, req_version):
            pkgversion_found = pkg.installed
        elif (pkg.candidate is not None
                  and check_req_version(pkg.candidate.version, req_version)):
            pkgversion_found = pkg.candidate
        else:
            # Sort by priority (as seen with `apt-cache policy <PackageName>`)
            pkgversions = sorted(pkg.versions, key=lambda v: v.policy_priority,
                                 reverse=True)
            for pkgversion in pkgversions:
                # Choose the first one that complies with required version
                if check_req_version(pkgversion.version, req_version):
                    pkgversion_found = pkgversion
                    break
    elif pkg.is_installed:
        pkgversion_found = pkg.installed
    else:
        pkgversion_found = pkg.candidate

    print("[buildpkg::get_version_to_install]"
          " Found complying version: {}".format(pkgversion_found))
    return pkgversion_found


# Input of this function is a single dependency, which is a list of alternatives
def check_deb_dependency_installable(dep_alts):
    for dep_alt in dep_alts:
        dep_name = dep_alt["name"]
        if APT_CACHE.has_key(dep_name):
            return True

    # Reaching here, none of the alternatives are installable
    return False


# Input of this function is a single dependency, which is a list of alternatives
def check_deb_dependency_installed(dep_alts):
    print("[buildpkg::check_deb_dependency_installed]"
          " Check Debian dependency installed:", dep2str(dep_alts))

    for dep_alt in dep_alts:
        dep_name = dep_alt["name"]
        dep_version = dep_alt["version"]
        dep_commit = dep_alt.setdefault("commit", None)

        if APT_CACHE.has_key(dep_name):
            pkg = APT_CACHE[dep_name]
            pkgversion = get_pkgversion_to_install(pkg, dep_version, dep_commit)
            if pkg.is_installed:
                if pkgversion == pkg.installed:
                    print("[buildpkg::check_deb_dependency_installed]"
                          " Dependency '{}' is installed via package '{}'"
                          " version '{}'".format(
                              dep2str(dep_alts), pkg.shortname, pkg.installed.version))
                    return True
                else:
                    print("[buildpkg::check_deb_dependency_installed] WARNING:"
                          " Dependency '{}' is installed via package '{}'"
                          " version '{}', but doesn't match requested version '{}'"
                          " or commit '{}'".format(
                              dep2str(dep_alts), pkg.shortname, pkg.installed.version,
                              dep_version, dep_commit))
            else:
                print("[buildpkg::check_deb_dependency_installed]"
                      " Dependency '{}' is not installed yet".format(
                          dep2str(dep_alts)))
        else:
            print("[buildpkg::check_deb_dependency_installed]"
                  " Dependency '{}' is not installable via `apt-get`".format(
                      dep2str(dep_alts)))

    # Reaching here, none of the alternatives are installed in a valid version
    return False


# Input of this function is a single dependency, which is a list of alternatives
def install_dependency(dep_alts):
    for dep_alt in dep_alts:
        pkg_name = dep_alt["name"]
        if not APT_CACHE.has_key(pkg_name):
            continue

        # Get package version to install that matches commit or version
        pkg = APT_CACHE[pkg_name]
        pkgversion = get_pkgversion_to_install(
            pkg, dep_alt["version"], dep_alt.setdefault("commit", None))

        if pkgversion is None:
            print("[buildpkg::install_dependency]"
                  " Dependency '{}' without any valid package version".format(
                      pkg_name))
            return False

        # Install selected dependency version
        print("[buildpkg::install_dependency]"
              " Install dependency '{}' via package '{}' version '{}'".format(
                  pkg_name, pkg.shortname, pkgversion.version))

        pkg.candidate = pkgversion
        pkg.mark_install()
        apt_cache_commit()

        if check_deb_dependency_installed(dep_alts):
            return True

    return False


def get_version():
    print("[buildpkg::get_version] Run 'kurento_get_version.sh'")
    try:
        cmd_out = subprocess.check_output(["kurento_get_version.sh"]).strip()
    except subprocess.CalledProcessError:
        print("[buildpkg::get_version] ERROR: Running 'kurento_get_version.sh'")
        return None
    else:
        print("[buildpkg::get_version] Found version:", cmd_out)
        cmd_out = cmd_out.split("-")[0]
        return cmd_out


def get_debian_version(simplify_dev_version, dist):
    version = get_version()
    if version is None:
        return None

    try:
        # Get either the latest tag, or the initial commit if no tags exist yet
        last_release = subprocess.check_output(
            "git describe --tags --abbrev=0"
            " || git rev-list --max-parents=0 HEAD", shell=True).strip()

        current_commit = subprocess.check_output(
            "git rev-parse --short HEAD", shell=True).strip()

        num_commits = subprocess.check_output(
            "git log " + last_release + "..HEAD --oneline"
            " | wc -l", shell=True).strip()
    except subprocess.CalledProcessError:
        # If Git fails (eg. the directory is not a Git repo), use default values
        current_commit = "0000000"
        num_commits = "1"  # Force a Development build in next code block

    now = datetime.fromtimestamp(time())

    if int(num_commits) > 0:
        # This is a Development build
        if simplify_dev_version:
            # Use "0" as the timestamp
            version = (version
                       + "." + dist
                       + "~0"
                       + "." + num_commits
                       + "." + current_commit)
        else:
            # Use a full timestamp
            version = (version
                       + "." + dist
                       + "~" + now.strftime("%Y%m%d%H%M%S")
                       + "." + num_commits
                       + "." + current_commit)
    else:
        # This is a Release build
        version = (version
                   + "." + dist
                   + "." + now.strftime("%Y%m%d%H%M%S")
                   + "." + current_commit)

    return version


def request_http(url, cert_path, key_path, file_path=None):
    if file_path is None:
        print("[buildpkg::request_http] Run request,"
              " URL: {}, cert file: {}, key file: {}".format(url, cert_path, key_path))
        try:
            res = requests.post(url, verify=False, cert=(cert_path, key_path))
            res.raise_for_status()
        except requests.RequestException as err:
            print("[buildpkg::request_http] ERROR: Running request:", err)
            exit(1)
        else:
            print("[buildpkg::request_http] DONE: Running request:\n", res.text)
    else:
        curl_cmd = ("curl --fail --insecure --key " + key_path
                    + " --cert " + cert_path
                    + " -X POST \"" + url + "\""
                    + " --data-binary @" + file_path)
        print("[buildpkg::request_http] Run command:", curl_cmd)
        try:
            curl_text = subprocess.check_output(curl_cmd, shell=True).strip()
        except subprocess.CalledProcessError:
            print("[buildpkg::request_http] ERROR: Running 'curl'")
            exit(1)
        else:
            print("[buildpkg::request_http] DONE: Running 'curl':\n", curl_text)


# Current code performs a 'form-encoded' upload ('Content-Type: application/x-www-form-urlencoded')
# Commented lines do a 'multipart' upload ('Content-Type: multipart/form-data')
# def request_http(url, cert_path, key_path, file_path=None):
#     file_obj = None
#     # files_dict = None

#     try:
#         file_obj = open(file_path, 'rb')
#     except TypeError:
#         pass  # file_path is None
#     except IOError as err:
#         print("[buildpkg::request_http] ERROR:"
#               " Opening file '{}', error: {}".format(file_path, err.strerror))
#         exit(1)
#     else:
#         print("[buildpkg::request_http] File opened:", file_path)
#         # files_dict = {os.path.basename(file_path): file_obj}

#     print("[buildpkg::request_http] Run request,"
#           " URL: {}, cert file: {}, key file: {}".format(url, cert_path, key_path))
#     try:
#         res = requests.post(url, data=file_obj,
#                             verify=False, cert=(cert_path, key_path))
#         # res = requests.post(url, files=files_dict,
#         #                     verify=False, cert=(cert_path, key_path))
#         res.raise_for_status()
#     except requests.RequestException as err:
#         print("[buildpkg::request_http] ERROR: Running request:", err)
#         exit(1)
#     else:
#         print("[buildpkg::request_http] DONE: Running request:\n", res.text)


def upload_package(args, buildconfig, dist, file_path, publish=False):
    if buildconfig.has_key("private") and buildconfig["private"] is True:
        repo = "ubuntu-priv"
    else:
        repo = "ubuntu-pub"

    base_url = (args.upload_url
                + "/upload?repo=" + repo
                + "&dist=" + dist
                + "&comp=" + args.component)
    upload_url = (base_url
                  + "&name=" + os.path.basename(file_path)
                  + "&cmd=add")

    print("[buildpkg::upload_package] Request HTTP upload")
    request_http(upload_url, args.cert.name, args.id_rsa.name, file_path)

    if publish:
        publish_url = base_url + "&cmd=publish"

        print("[buildpkg::upload_package] Request HTTP publish")
        request_http(publish_url, args.cert.name, args.id_rsa.name)


def install_build_dependencies():
    debctl = deb822.Deb822(
        open("debian/control"),
        fields=["Build-Depends", "Build-Depends-Indep"])

    builddep_str = debctl.get("Build-Depends")
    if debctl.has_key("Build-Depends-Indep"):
        builddep_str = builddep_str + "," + debctl.get("Build-Depends-Indep")

    # PkgRelation.parse_relations() returns a list that contains all
    # dependencies. Each one of these dependencies is a list of alternatives.
    relations = deb822.PkgRelation.parse_relations(builddep_str)

    print("[buildpkg::install_build_dependencies]"
          " Process dependencies: '{}'".format(dep2str(relations)))

    # Check if all required packages are installed
    for dep_alts in relations:
        if not check_deb_dependency_installed(dep_alts):
            if check_deb_dependency_installable(dep_alts):
                # Try to install missing dependencies
                print("[buildpkg::install_build_dependencies]"
                      " Try to install dependency '{}'".format(
                          dep2str(dep_alts)))
                if not install_dependency(dep_alts):
                    print("[buildpkg::install_build_dependencies] ERROR:"
                          " Installing dependency '{}'".format(
                              dep2str(dep_alts)))
            else:
                print("[buildpkg::install_build_dependencies]"
                      " Dependency '{}' has no package available, need to"
                      " download and build it".format(
                          dep2str(dep_alts)))


def generate_debian_package(args, buildconfig):
    global APT_CACHE

    project_name = args.project_name

    chglog = changelog.Changelog(open("debian/changelog"))
    old_chglog = changelog.Changelog(open("debian/changelog"))

    print("[buildpkg::generate_debian_package] ({})"
          " Run 'lsb_release'".format(project_name))
    try:
        dist = subprocess.check_output(["lsb_release", "-sc"]).strip()
    except subprocess.CalledProcessError:
        print("[buildpkg::generate_debian_package] ({})"
              " ERROR: Running 'lsb_release'".format(project_name))
        exit(1)
    else:
        print("[buildpkg::generate_debian_package] ({})"
              " Found distro code: {}".format(project_name, dist))

    print("[buildpkg::generate_debian_package] ({})"
          " Retrieve version from the project's metadata".format(project_name))
    new_version = get_debian_version(args.simplify_dev_version, dist)
    if new_version is None:
        print("[buildpkg::generate_debian_package] ({}) ERROR:"
              " No valid version in the project's metada".format(project_name))
        exit(1)

    chglog.new_block(version=new_version,
                     package=chglog.package,
                     distributions="testing",
                     changes=["\n  Generating new package version\n"],
                     author=chglog.author,
                     date=strftime("%a, %d %b %Y %H:%M:%S %z"),
                     urgency=chglog.urgency)

    chglog.write_to_open_file(open("debian/changelog", 'w'))

    # Execute commands defined in the build configuration file
    if buildconfig.has_key("prebuild-command"):
        print("[buildpkg::generate_debian_package] ({})"
              " Run prebuild-command: '{}'".format(
                  project_name, str(buildconfig["prebuild-command"])))
        try:
            subprocess.check_call(buildconfig["prebuild-command"], shell=True)
        except subprocess.CalledProcessError:
            print("[buildpkg::generate_debian_package] ({}) ERROR:"
                  " Running prebuild-command".format(project_name))
            exit(1)

    cpu_count = multiprocessing.cpu_count()

    print("[buildpkg::generate_debian_package] ({})"
          " Run 'dpkg-buildpackage', jobs: {}".format(project_name, cpu_count))
    try:
        subprocess.check_call(
            ["dpkg-buildpackage", "-uc", "-us", "-j" + str(cpu_count)])
    except subprocess.CalledProcessError:
        print("[buildpkg::generate_debian_package] ({}) ERROR:"
              " Running 'dpkg-buildpackage'".format(project_name))
        exit(1)

    paths_glob = "../*" + new_version + "_*.deb"
    file_paths = glob.glob(paths_glob)

    # Install the generated files (plus their installation dependencies)
    # FIXME: The Apt Python API (apt.debfile.DebPackage) doesn't have a
    # reliable method to do this.
    # Also there is the `gdebi` command, but it doesn't accept multiple files.
    print("[buildpkg::generate_debian_package] ({})"
          " Run 'dpkg -i' with generated files: {}".format(
              project_name, file_paths))
    if file_paths:
        root_privileges_gain()
        try:
            subprocess.check_call("dpkg -i " + paths_glob, shell=True)
        except subprocess.CalledProcessError:
            print("[buildpkg::generate_debian_package] ({})"
                  " 'dpkg -i' left unconfigured packages; try to solve that".format(
                      project_name))
            if subprocess.call(["apt-get", "install", "-f", "-y", "-q"]) != 0:
                print("[buildpkg::generate_debian_package] ({}) ERROR:"
                      " Running 'apt-get install -f'".format(project_name))
                exit(1)
        APT_CACHE.open()
        root_privileges_drop()

    if args.command == "upload":
        for file_path in file_paths:
            is_last = bool(file_path is file_paths[-1])

            if new_version.find("~") == -1 or args.force_release:
                upload_package(args, buildconfig, dist,
                               file_path, publish=is_last)
            elif args.force_testing:
                upload_package(args, buildconfig, dist+"-test",
                               file_path, publish=is_last)

            upload_package(args, buildconfig, dist+"-dev",
                           file_path, publish=is_last)

    if args.clean:
        file_paths = glob.glob("../*" + new_version + "*")
        for file_path in file_paths:
            os.remove(file_path)

    # Write old changelog to let everything as it was
    old_chglog.write_to_open_file(open("debian/changelog", 'w'))


def check_dependency_installed(dependency, debian_control_file):
    print("[buildpkg::check_dependency_installed]"
          " Dependency: '{}'".format(str(dependency)))

    ret_val = False

    while True:
        debctl = deb822.Deb822(debian_control_file)

        if len(debctl) == 0:
            break

        if debctl.has_key("Package"):
            pkg_name = debctl["Package"]
            dep = dependency
            dep["name"] = pkg_name

            ret_val = ret_val or check_deb_dependency_installed([dep])

    return ret_val


def compile_project(args):
    project_name = args.project_name
    project_workdir = os.getcwd()
    buildconfig_path = os.path.abspath(args.file)

    print("[buildpkg::compile_project]"
          " Project: '{}', work directory: '{}'".format(
              project_name, project_workdir))

    try:
        buildconfig_text = open(buildconfig_path, 'r').read()
    except IOError as err:
        print("[buildpkg::compile_project] ({}) ERROR:"
              " Reading build configuration file: '{}', error: {}".format(
                  project_name, buildconfig_path, err.strerror))
        exit(1)

    try:
        buildconfig = yaml.load(buildconfig_text)
    except yaml.YAMLError:
        print("[buildpkg::compile_project] ({}) ERROR:"
              " Parsing build configuration file: '{}'".format(
                  project_name, buildconfig_path))
        exit(1)

    print("[buildpkg::compile_project] ({})"
          " Check Debian build dependencies (from '{}')".format(
              project_name, os.path.abspath("debian/control")))
    install_build_dependencies()

    # Parse dependencies and check if corrects versions are found
    print("[buildpkg::compile_project] ({})"
          " Check build dependencies (from '{}')".format(
              project_name, buildconfig_path))

    # Parse dependencies defined in the build configuration file
    for dependency in buildconfig.get("dependencies", []):
        if not dependency.has_key("name"):
            print("[buildpkg::compile_project] ({}) ERROR:"
                  " Build dependency '{}' lacks a proper name".format(
                      project_name, str(dependency)))
            exit(1)
        if dependency.has_key("version"):
            regex = re.compile(r'(?P<relop>[>=<]+)\s*'
                               r'(?P<version>[0-9a-zA-Z:\-+~.]+)')
            match = regex.match(dependency["version"])
            if match:
                print("[buildpkg::compile_project] ({})"
                      " Parsed project dependency: '{}', version '{}'".format(
                          project_name, dependency["name"], dependency["version"]))
                parts = match.groupdict()
                dependency["version"] = (parts['relop'], parts['version'])
            else:
                print("[buildpkg::compile_project] ({}) ERROR:"
                      " Project dependency '{}' with invalid version string: '{}'".format(
                          project_name, dependency["name"], dependency["version"]))
                exit(1)
        else:
            print("[buildpkg::compile_project] ({})"
                  " Parsed project dependency: '{}', version: {}".format(
                      project_name, dependency["name"], "any"))
            dependency["version"] = None

    for dependency in buildconfig.get("dependencies", []):
        build_dependency_name = dependency["name"]
        git_url = args.base_url + "/" + build_dependency_name

        print("[buildpkg::compile_project] ({})"
              " Project dependency: '{}', check Debian info".format(
                  project_name, build_dependency_name))

        # TODO: Consolidate versions, check if commit is compatible with
        # version requirement and also if there is a newer commit
        if (dependency["version"] is None
                and (not dependency.has_key("commit")
                     or dependency["commit"] is None)):

            print("[buildpkg::compile_project] ({})"
                  " Run 'git ls-remote' ({})".format(project_name, git_url))
            try:
                cmd_out = subprocess.check_output(
                    "git ls-remote " + git_url + " HEAD", shell=True).strip()
            except subprocess.CalledProcessError:
                print("[buildpkg::compile_project] ({}) ERROR:"
                      " Running 'git ls-remote'".format(project_name))
                exit(1)
            else:
                dependency["commit"] = cmd_out.split()[0]

            print("[buildpkg::compile_project] ({})"
                  " Project dependency '{}' with no specific version or commit,"
                  " use Git HEAD: {}".format(
                      project_name, build_dependency_name, dependency["commit"]))

        # Load the file "debian/control" from the remote repo
        #
        # REVIEW: GitHub doesn't have support for `git archive`!
        # https://github.com/isaacs/github/issues/554
        # if dependency["commit"]:
        #     default_commit = dependency["commit"]
        # else:
        #     default_commit = "HEAD"
        # debian_control_file = os.popen(
        #     "git archive --remote=" + git_url
        #     + " " + default_commit + " debian/control")
        #
        # Workaround: use the SVN bridge API offered by GitHub.
        # TODO: Don't use "trunk", instead see if specific commit or version
        # can be used in the URL.

        svn_url = git_url + "/trunk/debian/control"
        print("[buildpkg::compile_project] ({})"
              " Run 'svn cat' ({})".format(project_name, svn_url))
        try:
            cmd_out = subprocess.check_output(
                "svn cat " + svn_url, shell=True).strip()
        except subprocess.CalledProcessError:
            print("[buildpkg::compile_project] ({}) ERROR:"
                  " Running 'svn cat':\n{}".format(project_name, cmd_out))
            exit(1)
        else:
            # Convert byte string from UTF-8 to Unicode text stream
            debian_control_file = io.StringIO(cmd_out.decode('utf-8'))

        if not check_dependency_installed(dependency, debian_control_file):
            print("[buildpkg::compile_project] ({})"
                  " Build dependency '{}' is not installed,"
                  " download and build it".format(
                      project_name, build_dependency_name))


            # ==== Change project: Compile the dependency ====

            os.chdir("..")
            repo = clone_repo(args.base_url, build_dependency_name)
            os.chdir(build_dependency_name)

            if (dependency["commit"] != None
                    and str(repo.commit()) != dependency["commit"]
                    and subprocess.call(["git", "checkout", dependency["commit"]]) != 0):
                print("[buildpkg::compile_project] ({}) ERROR:"
                      " Checking out the commit '{}' for build dependency '{}'".format(
                          project_name, dependency["commit"], build_dependency_name))
                exit(1)

            args.project_name = build_dependency_name
            compile_project(args)


            # ==== Change project: Resume working on the parent ====

            os.chdir(project_workdir)
            args.project_name = project_name


    #J REVIEW - With "true", kurento_check_version.sh creates and pushes a tag
    # from the current commit. But it doesn't have push permissions!
    # A tag must be done manually for now.
    # if os.system("kurento_check_version.sh true") != 0:
    print("[buildpkg::compile_project] ({})"
          " Run 'kurento_check_version.sh'".format(project_name))
    try:
        subprocess.check_call(["kurento_check_version.sh", "false"])
    except subprocess.CalledProcessError:
        print("[buildpkg::compile_project] ({}) ERROR:"
              " Running 'kurento_check_version.sh'".format(project_name))
        exit(1)

    print("[buildpkg::compile_project] ({})"
          " Build project and make Debian packages".format(project_name))
    generate_debian_package(args, buildconfig)

    print("[buildpkg::compile_project] ({}) Done.".format(project_name))


def print_uids():
    print("getresuid: {}".format(os.getresuid()))


def main():
    # Suppress requests for information during package configuration.
    # A completely unattended installation of a Debian package with 'apt-get'
    # is achieved by defining 'DEBIAN_FRONTEND="noninteractive"' and using
    # the options '--yes' and '--quiet' (ie. 'apt-get install -yq <...>').
    os.environ['DEBIAN_FRONTEND'] = "noninteractive"

    # Only raise to root privileges for the minimal time required
    # Good security practice!
    if os.geteuid() != 0:
        print("[buildpkg::main] ERROR: This program needs root privileges")
        exit(1)
    root_privileges_drop()

    parser = argparse.ArgumentParser(
        description=
        "Read configuration from .build.yaml and build the project")
    parser.add_argument("--file",
                        metavar="file",
                        help="Build configuration file (default: '{}')".format(
                            DEFAULT_CONFIG_FILE),
                        default=DEFAULT_CONFIG_FILE)
    parser.add_argument("--project_name",
                        help="Name of the project being built (for log)")
    parser.add_argument("--base_url",
                        metavar="base_url",
                        help="Base repository url",
                        required=True)
    parser.add_argument("--simplify_dev_version",
                        action="store_true",
                        help="Simplify dev version, useful for debugging")
    parser.add_argument("--clean",
                        action="store_true",
                        help="Clean generated files when finished")
    parser.add_argument("--no_apt_get_update",
                        action="store_true",
                        help="Do not perform an `apt-get update` on start")

    subparsers = parser.add_subparsers(dest="command")
    comp = subparsers.add_parser('compile', help='Compile package')
    upload = subparsers.add_parser('upload', help='Upload package')
    upload.add_argument(
        "--upload_url",
        help=
        "Url to upload the package (by default /upload is added to the url)",
        required=True)
    upload.add_argument("--component",
                        help="Component to upload the package",
                        required=True)
    upload.add_argument("--cert",
                        help="Certificate required to upload packages",
                        required=True,
                        type=argparse.FileType('r'))
    upload.add_argument("--id_rsa",
                        help="Key required to upload packages",
                        required=True,
                        type=argparse.FileType('r'))
    upload.add_argument("--force_release",
                        action="store_true",
                        help="Upload package to release repository"
                             " even if it is not a final release number")
    upload.add_argument("--force_testing",
                        action="store_true",
                        help="Upload package to testing repository")

    args = parser.parse_args()

    if not args.no_apt_get_update:
        print("[buildpkg::main] Run 'apt-get update'")
        apt_cache_update()

    if not args.project_name:
        args.project_name = os.path.basename(os.path.normpath(os.getcwd()))
    compile_project(args)


if __name__ == "__main__":
    main()
