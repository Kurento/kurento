#!/bin/bash

gdbbt() {
   tmp=$(tempfile);
   echo thread apply all bt >"$tmp";
   gdb -batch -nx -q -x "$tmp" -p "$1";
   rm -f "$tmp";
}

gdbbt $(pidof kurento-media-server) | cat