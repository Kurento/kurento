#!/usr/bin/python

import yaml
import argparse
import sys
#import json
import semver
import git
import os
from debian.deb822 import Deb822, PkgRelation
from apt.cache import Cache
import apt_pkg

from git import Repo

#a = aptsources.sourceslist.SourcesList()

# sudo apt-get install python-semver python-git python-yaml python-git python-apt python-debian
# python-simplejson

DEFAULT_CONFIG_FILE = '.build.yaml'

def clone_repo(base_url, repo_name):
  try:
    repo = Repo (repo_name)
    #for remote in repo.remotes:
      #remote.update()
  except:
    repo = Repo.clone_from (base_url + "/" + repo_name, repo_name)

  return repo

def check_dependency_installed (cache, dep):
  for dep_alternative in dep:
    name = dep_alternative["name"]
    if cache.has_key (name):
      pkg = cache[name]
      if pkg.is_installed:
        #Check if version is valid
        req_version = dep_alternative["version"]
        if req_version:
          comp = apt_pkg.version_compare (pkg.installed.version, req_version[1])
          if comp == 0 and req_version[0].find ("=") >= 0:
            return True
          if comp < 0 and req_version[0].find ("<") >= 0:
            return True
          if comp > 0 and req_version[0].find (">") >= 0:
            return True
        else:
          return True

  #If this code is reached, depdendency is not correctly installed in a valid version
  return False

def generate_debian_package(args):
  debfile = Deb822 (open("debian/control"), fields=["Build-Depends"])
  relations = PkgRelation.parse_relations (debfile.get("Build-Depends"))

  cache = Cache()

  #Check if all required packages are installed
  for dep in relations:
    if not check_dependency_installed (cache, dep):
      #Install not found dependencies
      print ("Dependency not matched: " + str(dep))

  #if os.system("dpkg-buildpackage -nc -uc -us") != 0:
    #print ("Error while generating package, try cleaning")
    #os.system ("dpkg-buildpackage -uc -us")

  # TODO: Install package

def compile_project (args):
  workdir = os.getcwd()
  print ("Compile project:" + workdir);

  try:
    f = open (args.file, 'r')

    config = yaml.load (f)
  except:
    print ("Config file not found")
    return

  # Parse dependencies and check if corrects versions are found
  if "dependencies" in config.keys():
    for dependency in config["dependencies"]:
      sub_project_name = dependency["name"]
      print (sub_project_name)

      os.chdir("..")
      repo = clone_repo (args.base_url, sub_project_name)
      os.chdir(sub_project_name)
      print (repo)
      if "version" in dependency.keys():
        print ("Version: " + dependency["version"])

      #If no version, master branch is used or latest package
      #If version is semver, try to find a package
      #if a gerrit version, transform to
      #If a commit try to get this commit from apt or compile if fails
      compile_project (args)

      os.chdir(workdir)

  generate_debian_package(args)

def main():
  parser = argparse.ArgumentParser (description="Read configurations from build.yaml")
  parser.add_argument ("--file", metavar="file", help="File to read config from", default=DEFAULT_CONFIG_FILE)
  parser.add_argument ("--base_url", metavar="base_url", help="Base repository url", required=True)

  args = parser.parse_args()

  compile_project (args)

if __name__ == "__main__":
  main()
