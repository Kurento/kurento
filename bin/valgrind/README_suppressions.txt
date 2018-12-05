External suppression files:

- debian.supp
  Generic issues with some core Debian libraries.
  Source: Installing package 'valgrind' from a Debian-based distribution.

- gst.supp
  GStreamer
  Source: GStreamer (https://github.com/GStreamer)
  File: https://github.com/GStreamer/common/blob/master/gst.supp

- walbottle.supp
  GLib and GIO
  Source: Walbottle project (https://github.com/pwithnall/walbottle)
  File: https://github.com/pwithnall/walbottle/blob/master/libwalbottle/tests/walbottle.supp

- GNOME.supp
  Suppressions from project GNOME.supp (https://github.com/dtrebbien/GNOME.supp)
  Generated all suppressions as per the project's instructions, and joined all together: `cat *.supp > GNOME.supp`.
