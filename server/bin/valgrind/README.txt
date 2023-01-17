Valgrind Suppressions
=====================

* /usr/lib/valgrind/debian.supp
  Core Debian libraries.
  Origin: Installing package 'valgrind' on Debian/Ubuntu.
  Source:
  - https://salsa.debian.org/debian/valgrind/-/raw/master/debian/supp/debian.supp

* /usr/share/glib-2.0/valgrind/glib.supp
  GLib, GObject, GIO.
  Origin: Installing package 'libglib2.0-dev' on Debian/Ubuntu.
  Source:
  - https://salsa.debian.org/gnome-team/glib/-/raw/ubuntu/master/glib.supp
  - https://gitlab.gnome.org/GNOME/glib/-/raw/main/tools/glib.supp

* /usr/share/doc/libgstreamer1.0-0-dbg/gst.supp
  GStreamer.
  Origin: Installing package 'libgstreamer1.0-0-dbg' on Debian/Ubuntu, and extracting it (gunzip).
  Source:
  - https://salsa.debian.org/gstreamer-team/gstreamer1.0/-/raw/master/tests/check/gstreamer.supp
  - https://gitlab.freedesktop.org/gstreamer/gstreamer/-/raw/main/subprojects/gstreamer/tests/check/gstreamer.supp



Other suppression files
=======================

Put here any other global suppression files that might be needed, and add them
as `--suppressions` flags to the `valgrind.conf.sh` file.
