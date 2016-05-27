#!/usr/bin/python2.7

import argparse
import gc
import glob
import os
import re
import subprocess
import sys
import urlparse
from datetime import datetime
from time import strftime, time

import git
import requests
from apt.cache import Cache
from debian.changelog import Changelog
from debian.deb822 import Deb822, PkgRelation
from git import Repo

import apt_pkg
import yaml

# sudo apt-get install curl python-git python-yaml python-apt python-debian python-requests git-review

DEFAULT_CONFIG_FILE = '.build.yaml'

#Next source is imported from git review

#Copyright (C) 2011-2012 OpenStack LLC.
#
#Licensed under the Apache License, Version 2.0 (the "License");
#you may not use this file except in compliance with the License.
#You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
#Unless required by applicable law or agreed to in writing, software
#distributed under the License is distributed on an "AS IS" BASIS,
#WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
#implied.
#
#See the License for the specific language governing permissions and
#limitations under the License.

urljoin = urlparse.urljoin
urlparse = urlparse.urlparse
do_input = raw_input

VERBOSE = False


class GitReviewException(Exception):
    pass


class CommandFailed(GitReviewException):
    def __init__(self, *args):
        Exception.__init__(self, *args)
        (self.rc, self.output, self.argv, self.envp) = args
        self.quickmsg = dict([
            ("argv", " ".join(self.argv)), ("rc", self.rc), ("output",
                                                             self.output)
        ])

    def __str__(self):
        return self.__doc__ + """
The following command failed with exit code %(rc)d
    "%(argv)s"
-----------------------
%(output)s
-----------------------""" % self.quickmsg


def run_command_status(*argv, **kwargs):
    if VERBOSE:
        print(datetime.datetime.now(), "Running:", " ".join(argv))
    if len(argv) == 1:
        # for python2 compatibility with shlex
        if sys.version_info < (3, ) and isinstance(argv[0], unicode):
            argv = shlex.split(argv[0].encode('utf-8'))
        else:
            argv = shlex.split(str(argv[0]))
    stdin = kwargs.pop('stdin', None)
    newenv = os.environ.copy()
    newenv['LANG'] = 'C'
    newenv['LANGUAGE'] = 'C'
    newenv.update(kwargs)
    p = subprocess.Popen(argv,
                         stdin=subprocess.PIPE if stdin else None,
                         stdout=subprocess.PIPE,
                         stderr=subprocess.STDOUT,
                         env=newenv)
    (out, nothing) = p.communicate(stdin)
    out = out.decode('utf-8', 'replace')
    return (p.returncode, out.strip())


def run_command_exc(klazz, *argv, **env):
    """Run command *argv, on failure raise klazz

    klazz should be derived from CommandFailed
    """
    (rc, output) = run_command_status(*argv, **env)
    if rc != 0:
        raise klazz(rc, output, argv, env)
    return output


def parse_gerrit_ssh_params_from_git_url(git_url):
    """Parse a given Git "URL" into Gerrit parameters. Git "URLs" are either
    real URLs or SCP-style addresses.
    """

    # The exact code for this in Git itself is a bit obtuse, so just do
    # something sensible and pythonic here instead of copying the exact
    # minutiae from Git.

    # Handle real(ish) URLs
    if "://" in git_url:
        parsed_url = urlparse(git_url)
        path = parsed_url.path

        hostname = parsed_url.netloc
        username = None
        port = parsed_url.port

        # Workaround bug in urlparse on OSX
        if parsed_url.scheme == "ssh" and parsed_url.path[:2] == "//":
            hostname = parsed_url.path[2:].split("/")[0]

        if "@" in hostname:
            (username, hostname) = hostname.split("@")
        if ":" in hostname:
            (hostname, port) = hostname.split(":")

        if port is not None:
            port = str(port)

    # Handle SCP-style addresses
    else:
        username = None
        port = None
        (hostname, path) = git_url.split(":", 1)
        if "@" in hostname:
            (username, hostname) = hostname.split("@", 1)

    # Strip leading slash and trailing .git from the path to form the project
    # name.
    project_name = re.sub(r"^/|(\.git$)", "", path)

    return (hostname, username, port, project_name)


def query_reviews(remote_url,
                  change=None,
                  current_patch_set=True,
                  exception=CommandFailed,
                  parse_exc=Exception):
    if remote_url.startswith('http://') or remote_url.startswith('https://'):
        query = query_reviews_over_http
    else:
        query = query_reviews_over_ssh
    return query(remote_url,
                 change=change,
                 current_patch_set=current_patch_set,
                 exception=exception,
                 parse_exc=parse_exc)


def query_reviews_over_http(remote_url,
                            change=None,
                            current_patch_set=True,
                            exception=CommandFailed,
                            parse_exc=Exception):
    url = urljoin(remote_url, '/changes/')
    if change:
        if current_patch_set:
            url += '?q=%s&o=CURRENT_REVISION' % change
        else:
            url += '?q=%s&o=ALL_REVISIONS' % change
    else:
        project_name = re.sub(r"^/|(\.git$)", "", urlparse(remote_url).path)
        params = urlencode({'q': 'project:%s status:open' % project_name})
        url += '?' + params

    if VERBOSE:
        print("Query gerrit %s" % url)
    request = run_http_exc(exception, url)
    if VERBOSE:
        print(request.text)
    reviews = json.loads(request.text[4:])

    # Reformat output to match ssh output
    try:
        for review in reviews:
            review["number"] = str(review.pop("_number"))
            if "revisions" not in review:
                continue
            patchsets = {}
            for key, revision in review["revisions"].items():
                fetch_value = list(revision["fetch"].values())[0]
                patchset = {"number": str(revision["_number"]),
                            "ref": fetch_value["ref"]}
                patchsets[key] = patchset
            review["patchSets"] = patchsets.values()
            review["currentPatchSet"] = patchsets[review["current_revision"]]
    except Exception as err:
        raise parse_exc(err)

    return reviews


def query_reviews_over_ssh(remote_url,
                           change=None,
                           current_patch_set=True,
                           exception=CommandFailed,
                           parse_exc=Exception):
    (hostname, username, port, project_name) = \
        parse_gerrit_ssh_params_from_git_url(remote_url)

    if change:
        if current_patch_set:
            query = "--current-patch-set change:%s" % change
        else:
            query = "--patch-sets change:%s" % change
    else:
        query = "project:%s status:open" % project_name

    port_data = "p%s" % port if port is not None else ""
    if username is None:
        userhost = hostname
    else:
        userhost = "%s@%s" % (username, hostname)

    if VERBOSE:
        print("Query gerrit %s %s" % (remote_url, query))
    output = run_command_exc(exception, "ssh", "-x" + port_data, userhost,
                             "gerrit", "query", "--format=JSON %s" % query)
    if VERBOSE:
        print(output)

    changes = []
    try:
        for line in output.split("\n"):
            if line[0] == "{":
                try:
                    data = json.loads(line)
                    if "type" not in data:
                        changes.append(data)
                except Exception:
                    if VERBOSE:
                        print(output)
    except Exception as err:
        raise parse_exc(err)
    return changes

# End of imported code from git review


def clone_repo(args, base_url, repo_name):
    try:
        repo = Repo(repo_name)
        print("Updating repo: " + repo_name)
        # TODO: Decide if current current branch should be updated
        for remote in repo.remotes:
            remote.update()
    except:
        print("Cloning repo: " + repo_name)
        repo = Repo.clone_from(base_url + "/" + repo_name, repo_name)

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
        version = get_version_to_install(pkg, dep_alternative["version"],
                                         dep_alternative["commit"])

        if version == None:
            print("Cannot find version of " + pkg_name +
                  " that matches the requirements")
            return False
        else:
            version = "=" + version

        # Install selected dependency version
        print("Installing " + pkg_name + version)
        if os.system(
                "sudo apt-get install --allow-change-held-packages --allow-downgrades -f -y -q "
                + pkg_name + version) != 0:
            os.system("sudo apt-get install --force-yes -f -y -q " + pkg_name +
                      version)
        cache = Cache()
        gc.collect()
        if check_deb_dependency_installed(cache, dep):
            return True

    return False


def get_version():
    version = os.popen("kurento_get_version.sh").read()
    return version


def get_debian_version(args, dist):
    version = get_version()

    last_release = os.popen(
        "git describe --abbrev=0 --tags || git rev-list --max-parents=0 HEAD").read(
        )
    last_release = last_release[:last_release.rfind("\n"):]
    current_commit = os.popen("git rev-parse --short HEAD").read()

    rc = os.popen("git log " + last_release + "..HEAD --oneline | wc -l").read(
    )
    rc = rc[:rc.rfind("\n"):]
    current_commit = current_commit[:current_commit.rfind("\n"):]
    version = version[:version.rfind("-"):]

    now = datetime.fromtimestamp(time())

    if int(rc) > 0:
        if args.simplify_dev_version:
            version = version + "~0." + rc + "." + current_commit + "." + dist
        else:
            version = version + "~" + now.strftime(
                "%Y%m%d%H%M%S") + "." + rc + "." + current_commit + "." + dist
    else:
        version = version + "." + dist

    return version


def request_http(url, cert, id_rsa, data=None):
    if data == None:
        r = requests.post(url, verify=False, cert=(cert, id_rsa))
        print(r.text)
    else:
        result = os.popen("curl --insecure --key " + id_rsa + "  --cert " +
                          cert + " -X POST \"" + url + "\" --data-binary @" +
                          data)
        print(result.read())


def upload_package(args, config, dist, package, publish=False):
    if config.has_key("private") and config["private"] == True:
        repo = "ubuntu-priv"
    else:
        repo = "ubuntu-pub"

    base_url = args.upload_url + "/upload?repo=" + \
        repo + "&dist=" + dist + "&comp=" + args.component
    upload_url = base_url + "&name=" + os.path.basename(package) + "&cmd=add"
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
                print("Dependency cannot be installed: " + PkgRelation.str([dep
                                                                            ]))
                exit(1)


def generate_debian_package(args, config):
    changelog = Changelog(open("debian/changelog"))
    old_changelog = Changelog(open("debian/changelog"))

    dist = os.popen("lsb_release -c").read()
    dist = dist[dist.rfind(":") + 1::].replace("\n", "").replace(
        "\t", "").replace(" ", "")

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
                "sudo apt-get install --allow-change-held-packages --allow-downgrades -f -y -q") != 0:
            os.system("sudo apt-get install --force-yes -f -y -q")
        if os.system("sudo dpkg -i ../*" + new_version + "_*.deb") != 0:
            print("Packages are not installable")
            exit(1)

    files = glob.glob("../*" + new_version + "_*.deb")
    if args.command == "upload":
        for f in files:
            if f is files[-1]:
                is_last = True
            else:
                is_last = False
            if new_version.find("~") == -1 or args.force_release:
                upload_package(args, config, dist, f, publish=is_last)
            upload_package(args, config, dist + "-dev", f, publish=is_last)

    if args.clean:
        files = glob.glob("../*" + new_version + "*")
        for f in files:
            os.remove(f)

    # Write old changelog to let everything as it was
    old_changelog.write_to_open_file(open("debian/changelog", 'w'))


def check_dependency_installed(cache, dependency, debian_control_str):
    print("Check dependency installed: " + str(dependency))

    ret_val = False

    #f = open("debian/control")
    while True:
        debfile = Deb822(debian_control_str)

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
        f = open(args.file, 'r')

        config = yaml.load(f)
    except:
        print("Config file not found: " + str(sys.exc_info()[0]))
        exit(1)

    cache = Cache()

    install_deb_dependencies(cache)

    cache = Cache()
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
                    print("Invalid version for dependency " + dependency[
                        "name"])
                    exit(1)
            else:
                dependency["version"] = None

        for dependency in config["dependencies"]:
            sub_project_name = dependency["name"]

            #Only revisions are allowed
            dependency["commit"] = None

            git_url = args.base_url + "/" + sub_project_name
            if dependency.has_key("review"):
                reviews = query_reviews(git_url, change=dependency["review"])
                if len(reviews) > 0 and reviews[0]["status"] == "MERGED":
                    dependency["commit"] = reviews[0]["currentPatchSet"][
                        "revision"]
                    print("Revision " + dependency["commit"])
            else:
                dependency["review"] = None

            # TODO: Consolidate versions, check if commit is compatible with
            # version requirement and also if there is a newer commit
            if dependency["commit"] == None and dependency["version"] == None:
                dependency["commit"] = str(os.popen("git ls-remote " + git_url
                                                    + " HEAD").read(7))

            if dependency["commit"]:
                default_commit = dependency["commit"]
            else:
                default_commit = "HEAD"

            debian_control_str = os.popen("git archive --remote=" + git_url +
                                          " " + default_commit +
                                          " debian/control")
            if not check_dependency_installed(cache, dependency,
                                              debian_control_str):
                os.chdir("..")
                repo = clone_repo(args, args.base_url, sub_project_name)
                os.chdir(sub_project_name)

                print("dependency " + dependency["name"] +
                      " not installed, compile it")
                if dependency["commit"] != None:
                    if str(repo.commit()) != dependency["commit"]:
                        if dependency["review"]:
                            if os.system("git review -d " + dependency[
                                    "review"]) != 0:
                                print("Cannot checkout to the commit " +
                                      dependency["commit"] + " for dependency "
                                      + dependency["name"])
                                exit(1)
                        else:
                            if os.system("git checkout " + dependency[
                                    "commit"]) != 0:
                                print("Cannot checkout to the commit " +
                                      dependency["commit"] + " for dependency "
                                      + dependency["name"])
                                exit(1)
                compile_project(args)
                os.chdir(workdir)

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
                        help="Upload package to release repository even if it is not a final release number")

    args = parser.parse_args()

    if not args.no_apt_get_update:
        os.system("sudo apt-get update")
    compile_project(args)


if __name__ == "__main__":
    main()
