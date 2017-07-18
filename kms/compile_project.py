#!/usr/bin/env python2.7
#pylint: disable=missing-docstring

# Install system tools and Python modules
# apt-get install --no-install-recommends \
#   curl wget git build-essential fakeroot debhelper subversion flex realpath \
#   python python-apt python-debian python-git python-requests python-yaml

# Module documentation
# - argparse: https://docs.python.org/2/library/argparse.html
# - python-apt: https://apt.alioth.debian.org/python-apt-doc/library/index.html
# - python-debian: http://python-debian.sourcearchive.com/documentation/0.1.21/hierarchy.html

from __future__ import print_function

import argparse
import gc
import glob
import os
import re
from datetime import datetime
from time import strftime, time

import apt
import git
import requests

from debian.changelog import Changelog
from debian.deb822 import Deb822, PkgRelation

import apt_pkg
import yaml

DEFAULT_CONFIG_FILE = '.build.yaml'


def clone_repo(base_url, repo_name):
    try:
        repo = git.Repo(repo_name)
        print("[buildpkg::clone_repo] Update repo: " + repo_name)
        # TODO: Decide if current branch should be updated
        for remote in repo.remotes:
            remote.update()
    except:
        print("[buildpkg::clone_repo] Clone repo: " + repo_name)
        repo = git.Repo.clone_from(base_url + "/" + repo_name, repo_name)

    return repo


def get_version_to_install(pkg, req_version, commit):
    valid_versions = []
    for version in pkg.versions:
        if commit:
            if version.version.find(commit[:7]) >= 0:
                valid_versions.append(version.version)
        elif req_version:
            comp = apt_pkg.version_compare(version.version, req_version[1])
            if comp == 0 and req_version[0].find("=") >= 0:
                valid_versions.append(version.version)
            if comp < 0 and req_version[0].find("<") >= 0:
                valid_versions.append(version.version)
            if comp > 0 and req_version[0].find(">") >= 0:
                valid_versions.append(version.version)
        else:
            valid_versions.append(version.version)

    # As list of versions seems to be correctly sorted, get the first one
    if len(valid_versions) > 0:
        return valid_versions[0]
    else:
        return None


def check_deb_dependency_installable(cache, dep):
    for dep_alternative in dep:
        dep_name = dep_alternative["name"]
        if cache.has_key(dep_name):
            return True

    # Reaching here, none of the alternatives are installable
    return False


def check_deb_dependency_installed(cache, dep):
    print("[buildpkg::check_deb_dependency_installed]"
          " Check Debian dependency installed: " + str(dep))

    for dep_alternative in dep:
        dep_name = dep_alternative["name"]
        dep_alternative.setdefault("commit")
        dep_version = dep_alternative["version"]
        dep_commit = dep_alternative["commit"]

        if cache.has_key(dep_name):
            pkg = cache[dep_name]
            if pkg.is_installed:
                # Check if version is valid
                version = get_version_to_install(pkg, dep_version, dep_commit)
                if version == pkg.installed.version:
                    print("[buildpkg::check_deb_dependency_installed]"
                          " Dependency: '{}' is installed via package: {},"
                          " version: {}".format(
                              dep_name, pkg.shortname, pkg.installed.version))
                    return True
                else:
                    print("[buildpkg::check_deb_dependency_installed] WARNING:"
                          " Dependency: '{}' is installed via package: {},"
                          " version: {}, but doesn't match requested version: {}"
                          " or commit: {}".format(
                              dep_name, pkg.shortname, pkg.installed.version,
                              dep_version, dep_commit))
            else:
                print("[buildpkg::check_deb_dependency_installed]"
                      " Dependency: '{}' is not yet installed".format(dep_name))
        else:
            print("[buildpkg::check_deb_dependency_installed]"
                  " Dependency: '{}' is not installable via apt-get".format(dep_name))

    # Reaching here, none of the alternatives are installed in a valid version
    return False


def install_dependency(cache, dep):
    for dep_alternative in dep:
        pkg_name = dep_alternative["name"]
        if not cache.has_key(pkg_name):
            continue

        # Get package version to install that matches commit or version
        pkg = cache[pkg_name]
        dep_alternative.setdefault("commit")
        version = get_version_to_install(
            pkg, dep_alternative["version"], dep_alternative["commit"])

        if version is None:
            print("[buildpkg::install_dependency]"
                  " Dependency: '{}' without any valid package version".format(
                      pkg_name))
            return False
        else:
            version = "=" + version

        # Install selected dependency version
        print("[buildpkg::install_dependency]"
              " Install dependency: '{}' via package: {}, version: {}".format(
                  pkg_name, pkg.shortname, version))
        os.environ["DEBIAN_FRONTEND"] = "noninteractive"
        if os.system("sudo apt-get install"
                     " --allow-change-held-packages"
                     " --allow-downgrades -f -y -q "
                     + pkg_name + version) != 0:
            os.system("sudo apt-get install"
                      " --force-yes -f -y -q "
                      + pkg_name + version)
        cache = apt.Cache()
        gc.collect()
        if check_deb_dependency_installed(cache, dep):
            return True

    return False


def get_version():
    print("[buildpkg::get_version] Run 'kurento_get_version.sh'")
    cmd_file = os.popen("kurento_get_version.sh")
    cmd_out = cmd_file.read().strip()

    if cmd_file.close() is None:
        print("[buildpkg::get_version] Found version: " + cmd_out)
        return cmd_out
    else:
        print("[buildpkg::get_version] ERROR: Running 'kurento_get_version.sh'")
        return None


def get_debian_version(simplify_dev_version, dist):
    version = get_version()
    if version is None:
        return None

    version = version[:version.rfind("-"):]

    # Get either the latest tag, or the initial commit if no tags exist yet
    last_release = (os.popen(
        "git describe --tags --abbrev=0 || git rev-list --max-parents=0 HEAD")
                    .read())
    last_release = last_release[:last_release.rfind("\n"):]

    current_commit = os.popen(
        "git rev-parse --short HEAD").read()
    current_commit = current_commit[:current_commit.rfind("\n"):]

    num_commits = os.popen(
        "git log " + last_release + "..HEAD --oneline | wc -l").read()
    num_commits = num_commits[:num_commits.rfind("\n"):]

    now = datetime.fromtimestamp(time())

    if int(num_commits) > 0:
        # This is a Development build
        if simplify_dev_version:
            version = (version
                       + "." + dist
                       + "~0." + num_commits
                       + "." + current_commit)
        else:
            version = (version
                       + "." + dist
                       + "~" + now.strftime("%Y%m%d%H%M%S")
                       + "." + num_commits
                       + "." + current_commit)
    else:
        # This is a Release build
        # FIXME - this is a hack done to allow multiple builds of a package
        # with the same version number, a consecuence of using a linear
        # pipeline of dependent jobs in CI.
        # With an ideal workflow for releases, the version numbers should
        # be something like "6.6.2" and not "6.6.2-20170605185155"
        version = (version
                   + "." + dist
                   + "." + now.strftime("%Y%m%d%H%M%S")
                   + "." + current_commit)

    return version


def request_http(url, cert, id_rsa, data=None):
    if data is None:
        req = requests.post(url, verify=False, cert=(cert, id_rsa))
        print(req.text)
    else:
        result = os.popen("curl --insecure --key " + id_rsa
                          + "  --cert " + cert
                          + " -X POST \"" + url
                          + "\" --data-binary @" + data)
        print(result.read())


def upload_package(args, buildconfig, dist, package, publish=False):
    if buildconfig.has_key("private") and buildconfig["private"] is True:
        repo = "ubuntu-priv"
    else:
        repo = "ubuntu-pub"

    base_url = (args.upload_url
                + "/upload?repo=" + repo
                + "&dist=" + dist
                + "&comp=" + args.component)
    upload_url = (base_url
                  + "&name=" + os.path.basename(package)
                  + "&cmd=add")
    print("[buildpkg::upload_package] URL: " + upload_url)

    request_http(upload_url, args.cert.name, args.id_rsa.name, package)

    if publish:
        publish_url = base_url + "&cmd=publish"
        print("[buildpkg::upload_package] Publish URL: " + publish_url)
        request_http(publish_url, args.cert.name, args.id_rsa.name)


def install_deb_dependencies(cache):
    debfile = Deb822(
        open("debian/control"),
        fields=["Build-Depends", "Build-Depends-Indep"])

    rel_str = debfile.get("Build-Depends")
    if debfile.has_key("Build-Depends-Indep"):
        rel_str = rel_str + "," + debfile.get("Build-Depends-Indep")

    relations = PkgRelation.parse_relations(rel_str)

    # Check if all required packages are installed
    for dep in relations:
        if not check_deb_dependency_installed(cache, dep):
            if check_deb_dependency_installable(cache, dep):
                # Try to install missing dependencies
                print("[buildpkg::install_deb_dependencies]"
                      " Try to install dependency: '{}'".format(
                          PkgRelation.str([dep])))
                if not install_dependency(cache, dep):
                    print("[buildpkg::install_deb_dependencies] ERROR:"
                          " Installing dependency: '{}'".format(
                              PkgRelation.str([dep])))
            else:
                print("[buildpkg::install_deb_dependencies]"
                      " Dependency: '{}' package not available, need to"
                      " download and built it".format(
                          PkgRelation.str([dep])))


def generate_debian_package(args, buildconfig):
    project_name = args.project_name
    changelog = Changelog(open("debian/changelog"))
    old_changelog = Changelog(open("debian/changelog"))

    dist = os.popen("lsb_release -c").read()
    dist = (dist[dist.rfind(":") + 1::]
            .replace("\n", "").replace("\t", "").replace(" ", ""))

    print("[buildpkg::generate_debian_package] ({})"
          " Retrieve version from the project's metadata".format(project_name))
    new_version = get_debian_version(args.simplify_dev_version, dist)
    if new_version is None:
        print("[buildpkg::generate_debian_package] ({}) ERROR:"
              " No valid version in the project's metada".format(project_name))
        exit(1)

    changelog.new_block(version=new_version,
                        package=changelog.package,
                        distributions="testing",
                        changes=["\n  Generating new package version\n"],
                        author=changelog.author,
                        date=strftime("%a, %d %b %Y %H:%M:%S %z"),
                        urgency=changelog.urgency)

    changelog.write_to_open_file(open("debian/changelog", 'w'))

    # Execute commands defined in the build configuration file
    if buildconfig.has_key("prebuild-command"):
        print("[buildpkg::generate_debian_package] ({})"
              " Run 'prebuild-command': {}".format(
                  project_name, str(buildconfig["prebuild-command"])))
        if os.system(buildconfig["prebuild-command"]) != 0:
            print("[buildpkg::generate_debian_package] ({}) ERROR:"
                  " Running 'prebuild-command'".format(project_name))
            exit(1)

    print("[buildpkg::generate_debian_package] ({})"
          " Run 'dpkg-buildpackage'".format(project_name))
    if os.system("dpkg-buildpackage -uc -us") != 0:
        print("[buildpkg::generate_debian_package] ({}) ERROR:"
              " Running 'dpkg-buildpackage'".format(project_name))
        exit(1)

    print("[buildpkg::generate_debian_package] ({})"
          " Run 'dpkg -i'".format(project_name))
    if os.system("sudo dpkg -i ../*" + new_version + "_*.deb") != 0:
        os.environ["DEBIAN_FRONTEND"] = "noninteractive"
        if os.system(
                "sudo apt-get install"
                " --allow-change-held-packages"
                " --allow-downgrades -f -y -q") != 0:
            os.system("sudo apt-get install --force-yes -f -y -q")
        if os.system("sudo dpkg -i ../*" + new_version + "_*.deb") != 0:
            print("[buildpkg::generate_debian_package] ({}) ERROR:"
                  " Resulting packages are not installable".format(project_name))
            exit(1)

    files = glob.glob("../*" + new_version + "_*.deb")
    if args.command == "upload":
        for afile in files:
            if afile is files[-1]:
                is_last = True
            else:
                is_last = False

            if new_version.find("~") == -1 or args.force_release:
                upload_package(args, buildconfig, dist, afile, publish=is_last)
            elif args.force_testing:
                upload_package(args, buildconfig, dist+"-test",
                               afile, publish=is_last)

            upload_package(args, buildconfig, dist+"-dev",
                           afile, publish=is_last)

    if args.clean:
        files = glob.glob("../*" + new_version + "*")
        for afile in files:
            os.remove(afile)

    # Write old changelog to let everything as it was
    old_changelog.write_to_open_file(open("debian/changelog", 'w'))


def check_dependency_installed(cache, dependency, debian_control_file):
    print("[buildpkg::check_dependency_installed]"
          " Dependency: '{}'".format(str(dependency)))

    ret_val = False

    while True:
        debfile = Deb822(debian_control_file)

        if len(debfile) == 0:
            break

        if debfile.has_key("Package"):
            pkg_name = debfile["Package"]
            dep = dependency
            dep["name"] = pkg_name

            ret_val = ret_val or check_deb_dependency_installed(cache, [dep])

    return ret_val


def compile_project(args):
    project_workdir = os.getcwd()
    project_name = os.path.basename(os.path.normpath(project_workdir))
    vars(args)["project_name"] = project_name

    buildconfig_path = os.path.abspath(args.file)

    print("[buildpkg::compile_project]"
          " Project: {}, work directory: '{}'".format(
              project_name, project_workdir))

    try:
        buildconfig_file = open(buildconfig_path, 'r')
        buildconfig = yaml.load(buildconfig_file)
    except IOError:
        print("[buildpkg::compile_project] ({}) ERROR:"
              " Opening build configuration file: '{}'".format(
                  project_name, buildconfig_path))
        exit(1)

    cache = apt.Cache()

    print("[buildpkg::compile_project] ({})"
          " Check Debian packages (from '{}')".format(
              project_name, os.path.abspath("debian/control")))
    install_deb_dependencies(cache)

    cache = apt.Cache()
    gc.collect()

    # Parse dependencies and check if corrects versions are found
    print("[buildpkg::compile_project] ({})"
          " Check build dependencies (from '{}')".format(
              project_name, buildconfig_path))

    # Parse dependencies defined in the build configuration file
    for dependency in buildconfig.get("dependencies", []):
        if not dependency.has_key("name"):
            print("[buildpkg::compile_project] ({}) ERROR:"
                  " Build dependency lacks a name: '{}'".format(
                      project_name, str(dependency)))
            exit(1)
        if dependency.has_key("version"):
            regex = re.compile(r'(?P<relop>[>=<]+)\s*'
                               r'(?P<version>[0-9a-zA-Z:\-+~.]+)')
            match = regex.match(dependency["version"])
            if match:
                print("[buildpkg::compile_project] ({})"
                      " Look for build dependency: '{}', version: {}".format(
                          project_name, dependency["name"], dependency["version"]))
                parts = match.groupdict()
                dependency["version"] = (parts['relop'], parts['version'])
            else:
                print("[buildpkg::compile_project] ({}) ERROR:"
                      " Build dependency: '{}' with invalid version string: '{}'".format(
                          project_name, dependency["name"], dependency["version"]))
                exit(1)
        else:
            dependency["version"] = None

    for dependency in buildconfig.get("dependencies", []):
        build_dependency_name = dependency["name"]
        git_url = args.base_url + "/" + build_dependency_name

        print("[buildpkg::compile_project] ({})"
              " Build dependency: '{}', check Debian packages (from '{}')".format(
                  project_name, build_dependency_name, git_url + "/debian/control"))

        # TODO: Consolidate versions, check if commit is compatible with
        # version requirement and also if there is a newer commit
        if (dependency["version"] is None
                and (not dependency.has_key("commit")
                     or dependency["commit"] is None)):

            dependency["commit"] = str(os.popen(
                "git ls-remote " + git_url + " HEAD").read(7))

            print("[buildpkg::compile_project] ({})"
                  " Build dependency: '{}' with no specific version or commit,"
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
        # Workaround: use the SVN bridge API offered by GitHub:
        debian_control_file = os.popen(
            "svn cat " + git_url + "/trunk/debian/control")

        if not check_dependency_installed(cache, dependency, debian_control_file):
            print("[buildpkg::compile_project] ({})"
                  " Build dependency: '{}' is not installed,"
                  " download and build it".format(
                      project_name, build_dependency_name))

            os.chdir("..")
            repo = clone_repo(args.base_url, build_dependency_name)
            os.chdir(build_dependency_name)

            if (dependency["commit"] != None
                    and str(repo.commit()) != dependency["commit"]
                    and os.system("git checkout " + dependency["commit"]) != 0):
                print("[buildpkg::compile_project] ({}) ERROR:"
                      " Checking out the commit: {} for build dependency: '{}'".format(
                          project_name, dependency["commit"], build_dependency_name))
                exit(1)
            compile_project(args)
            os.chdir(project_workdir)

    #J REVIEW - With "true", kurento_check_version.sh creates and pushes a tag
    # from the current commit. But it doesn't have push permissions!
    # A tag must be done manually for now.
    # if os.system("kurento_check_version.sh true") != 0:
    print("[buildpkg::compile_project] ({})"
          " Run 'kurento_check_version.sh'".format(project_name))
    if os.system("kurento_check_version.sh false") != 0:
        print("[buildpkg::compile_project] ({}) ERROR:"
              " Running 'kurento_check_version.sh'".format(project_name))
        exit(1)

    print("[buildpkg::compile_project] ({})"
          " Build project and make Debian packages".format(project_name))
    generate_debian_package(args, buildconfig)

    print("[buildpkg::compile_project] ({}) Done.".format(project_name))


def main():
    parser = argparse.ArgumentParser(
        description=
        "Read configuration from .build.yaml and build the project")
    parser.add_argument("--file",
                        metavar="file",
                        help="Build configuration file (default: '{}')".format(
                            DEFAULT_CONFIG_FILE),
                        default=DEFAULT_CONFIG_FILE)
    parser.add_argument("--base_url",
                        metavar="base_url",
                        help="Base repository url",
                        required=True)
    parser.add_argument("--simplify_dev_version",
                        action="store_true",
                        help="Simplify dev version, usefull for debugging")
    parser.add_argument("--clean",
                        action="store_true",
                        help="Clean generated files when finished")
    parser.add_argument("--no_apt_get_update",
                        action="store_true",
                        help="Do not perform an apt-get update on start")

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
        os.system("sudo apt-get update")
    compile_project(args)


if __name__ == "__main__":
    main()
