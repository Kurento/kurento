#!/usr/bin/env python2.7
#pylint: disable=missing-docstring

from __future__ import print_function

import argparse
import gc
import glob
import os
import re
import sys
from datetime import datetime
from time import strftime, time

import apt
import git
import requests

from debian.changelog import Changelog
from debian.deb822 import Deb822, PkgRelation

import apt_pkg
import yaml

# sudo apt-get install curl python-apt python-debian python-git python-requests python-yaml

DEFAULT_CONFIG_FILE = '.build.yaml'


def clone_repo(base_url, repo_name):
    try:
        repo = git.Repo(repo_name)
        print("Updating repo: " + repo_name)
        # TODO: Decide if current current branch should be updated
        for remote in repo.remotes:
            remote.update()
    except:
        print("Cloning repo: " + repo_name)
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


def check_dep(cache, pkg_name, req_version, commit):
    if cache.has_key(pkg_name):
        pkg = cache[pkg_name]

        if pkg.is_installed:
            # Check if version is valid
            version = get_version_to_install(pkg, req_version, commit)
            return version == pkg.installed.version
    return False


def check_deb_dependency_installed(cache, dep):
    for dep_alternative in dep:
        name = dep_alternative["name"]
        dep_alternative.setdefault("commit")

        if check_dep(cache, name, dep_alternative["version"],
                     dep_alternative["commit"]):
            return True

    # If this code is reached, depdendency is not correctly installed in a valid version
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
            print("Cannot find version of " + pkg_name +
                  " that matches the requirements")
            return False
        else:
            version = "=" + version

        # Install selected dependency version
        print("Installing " + pkg_name + version)
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
    version = os.popen("kurento_get_version.sh").read()
    #J TODO: Handle errors
    return version


def get_debian_version(args, dist):
    version = get_version()
    version = version[:version.rfind("-"):]

    last_release = (os.popen(
        "git describe --abbrev=0 --tags || git rev-list --max-parents=0 HEAD")
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
        if args.simplify_dev_version:
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
    # else:
    #     # This is a Release build
    #     version = (version
    #                + "." + dist
    #                + "." + now.strftime("%Y%m%d%H%M%S")
    #                + "." + current_commit)

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


def upload_package(args, config, dist, package, publish=False):
    if config.has_key("private") and config["private"] is True:
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
    print("url: " + upload_url)

    request_http(upload_url, args.cert.name, args.id_rsa.name, package)

    if publish:
        publish_url = base_url + "&cmd=publish"
        print("publish url: " + publish_url)
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
            # Install not found dependencies
            print("Dependency not matched: " + str(dep))
            if not install_dependency(cache, dep):
                print("Dependency cannot be installed: " + PkgRelation.str([dep]))


def generate_debian_package(args, config):
    changelog = Changelog(open("debian/changelog"))
    old_changelog = Changelog(open("debian/changelog"))

    dist = os.popen("lsb_release -c").read()
    dist = (dist[dist.rfind(":") + 1::]
            .replace("\n", "").replace("\t", "").replace(" ", ""))

    new_version = get_debian_version(args, dist)

    changelog.new_block(version=new_version,
                        package=changelog.package,
                        distributions="testing",
                        changes=["\n  Generating new package version\n"],
                        author=changelog.author,
                        date=strftime("%a, %d %b %Y %H:%M:%S %z"),
                        urgency=changelog.urgency)

    changelog.write_to_open_file(open("debian/changelog", 'w'))

    # Execute commands defined in config:
    if config.has_key("prebuild-command"):
        print("Executing prebuild-command: " + str(config["prebuild-command"]))
        if os.system(config["prebuild-command"]) != 0:
            print("Failed to execute prebuild command")
            exit(1)

    if os.system("dpkg-buildpackage -uc -us") != 0:
        print("Error generating package")
        exit(1)

    if os.system("sudo dpkg -i ../*" + new_version + "_*.deb") != 0:
        if os.system(
                "sudo apt-get install"
                " --allow-change-held-packages"
                " --allow-downgrades -f -y -q") != 0:
            os.system("sudo apt-get install --force-yes -f -y -q")
        if os.system("sudo dpkg -i ../*" + new_version + "_*.deb") != 0:
            print("Packages are not installable")
            exit(1)

    files = glob.glob("../*" + new_version + "_*.deb")
    if args.command == "upload":
        for afile in files:
            if afile is files[-1]:
                is_last = True
            else:
                is_last = False
            if new_version.find("~") == -1 or args.force_release:
                upload_package(args, config, dist, afile, publish=is_last)
            upload_package(args, config, dist + "-dev", afile, publish=is_last)

    if args.clean:
        files = glob.glob("../*" + new_version + "*")
        for afile in files:
            os.remove(afile)

    # Write old changelog to let everything as it was
    old_changelog.write_to_open_file(open("debian/changelog", 'w'))


def check_dependency_installed(cache, dependency, debian_control_file):
    print("Check dependency installed: " + str(dependency))

    ret_val = False

    #f = open("debian/control")
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
    workdir = os.getcwd()
    print("Compile project: " + workdir)

    try:
        config_file = open(args.file, 'r')
        config = yaml.load(config_file)
    except:
        print("Config file not found: " + str(sys.exc_info()[0]))
        exit(1)

    cache = apt.Cache()

    install_deb_dependencies(cache)

    cache = apt.Cache()
    gc.collect()

    # Parse dependencies and check if corrects versions are found
    if config.has_key("dependencies"):
        # Parse dependencies config
        for dependency in config["dependencies"]:
            if not dependency.has_key("name"):
                print("dependency: >" + str(dependency) + "<\n needs a name")
                exit(1)
            if dependency.has_key("version"):
                regex = re.compile(r'(?P<relop>[>=<]+)\s*'
                                   r'(?P<version>[0-9a-zA-Z:\-+~.]+)')
                match = regex.match(dependency["version"])
                if match:
                    parts = match.groupdict()
                    dependency["version"] = (parts['relop'], parts['version'])
                else:
                    print("Invalid version for dependency " + dependency["name"])
                    exit(1)
            else:
                dependency["version"] = None

        for dependency in config["dependencies"]:
            sub_project_name = dependency["name"]

            #Only revisions are allowed
            dependency["commit"] = None

            git_url = args.base_url + "/" + sub_project_name

            # TODO: Consolidate versions, check if commit is compatible with
            # version requirement and also if there is a newer commit
            if (dependency["commit"] is None
                    and dependency["version"] is None):
                dependency["commit"] = str(os.popen(
                    "git ls-remote " + git_url + " HEAD").read(7))

            #J
            # Load from the remote repo the file "debian/control"
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

            if not check_dependency_installed(cache, dependency,
                                              debian_control_file):
                os.chdir("..")
                repo = clone_repo(args.base_url, sub_project_name)
                os.chdir(sub_project_name)

                print("dependency " + dependency["name"]
                      + " not installed, compile it")
                if (dependency["commit"] != None
                        and str(repo.commit()) != dependency["commit"]
                        and os.system("git checkout " + dependency["commit"]) != 0):
                    print("Cannot checkout to the commit " + dependency["commit"]
                          + " for dependency " + dependency["name"])
                    exit(1)
                compile_project(args)
                os.chdir(workdir)

    #J REVIEW - With "true", kurento_check_version.sh creates and pushes a tag
    # from the current commit. But it doesn't have push permissions!
    # A tag must be done manually for now.
    # if os.system("kurento_check_version.sh true") != 0:
    if os.system("kurento_check_version.sh false") != 0:
        print("Error while checking the version")
        exit(1)
    generate_debian_package(args, config)


def main():
    parser = argparse.ArgumentParser(
        description=
        "Read configurations from .build.yaml and builds the project")
    parser.add_argument("--file",
                        metavar="file",
                        help="File to read config from",
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

    args = parser.parse_args()

    if not args.no_apt_get_update:
        os.system("sudo apt-get update")
    compile_project(args)


if __name__ == "__main__":
    main()
