#!/usr/bin/python

import yaml
import argparse
import sys
import json

DEFAULT_CONFIG_FILE = '.build.yaml'

parser = argparse.ArgumentParser (description="Read configurations from build.yaml")
parser.add_argument ("--file", metavar="file", help="File to read config from", default=DEFAULT_CONFIG_FILE)
group = parser.add_mutually_exclusive_group(required=True)
group.add_argument ("--key", metavar="key", help="Key to be printed")
group.add_argument ("--all", action="count", help="Print all keys")


args = parser.parse_args()

try:
  f = open (args.file, 'r')

  config = yaml.load (f)
except:
  sys.exit(0);

if args.all > 0:
  print (json.dumps(config, indent=2, sort_keys=True))
else:
  try:
    print (config[args.key])
  except:
    print (0);

