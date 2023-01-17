#!/usr/bin/python3

import sys
import gi

gi.require_version('Gst', '1.0')
from gi.repository import Gst
from gi.repository import GLib
from gi.repository import Gtk

def bus_callback(bus, message, not_used):
  t = message.type
  if t == Gst.MessageType.EOS:
    sys.stdout.write("End-of-stream\n")
    Gtk.main_quit()
  elif t == Gst.MessageType.ERROR:
    err, debug = message.parse_error()
    sys.stderr.write("Error: %s: %s\n" % (err, debug))
    element = message.src
    #Gtk.main_quit()

  return True

class MyWindow(Gtk.Window):

  def __init__(self):
    Gtk.Window.__init__(self, title="Screen size changes test")

    hbox = Gtk.Box(spacing=10)
    hbox.set_homogeneous(False)
    vbox_left = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=10)
    vbox_left.set_homogeneous(False)
    vbox_right = Gtk.Box(orientation=Gtk.Orientation.VERTICAL, spacing=10)
    vbox_right.set_homogeneous(False)

    self.add (hbox)

    hbox.pack_start(vbox_left, True, True, 0)
    hbox.pack_start(vbox_right, True, True, 0)

    adjustment = Gtk.Adjustment(320, 1, 1280, 10, 100, 0)
    self.width_spin = Gtk.SpinButton()
    self.width_spin.set_adjustment(adjustment)
    self.width_spin.set_value (self.width_spin.get_value())
    vbox_right.pack_start(self.width_spin, True, True, 0)
    width_label = Gtk.Label("Width")
    vbox_left.pack_start(width_label, True, True, 0)

    adjustment = Gtk.Adjustment(240, 1, 1280, 10, 100, 0)
    self.height_spin = Gtk.SpinButton()
    self.height_spin.set_adjustment(adjustment)
    self.height_spin.set_value (self.height_spin.get_value())
    vbox_right.pack_start(self.height_spin, True, True, 0)
    height_label = Gtk.Label("Height")
    vbox_left.pack_start(height_label, True, True, 0)

    caps_change = Gtk.Button(label="Change Size")
    caps_change.connect("clicked", self.on_caps_change_clicked)
    hbox.pack_start(caps_change, True, True, 0)

    adjustment = Gtk.Adjustment(1000, 100, 10000000, 100, 1000, 0)
    self.bitrate_spin = Gtk.SpinButton()
    self.bitrate_spin.set_adjustment(adjustment)
    self.bitrate_spin.set_value (self.bitrate_spin.get_value())
    hbox.pack_start(self.bitrate_spin, True, True, 0)
    change_bitrate = Gtk.Button(label="Change Bitrate")
    change_bitrate.connect("clicked", self.on_change_bitrate_clicked)
    hbox.pack_start(change_bitrate, True, True, 0)

    self.create_pipeline()

    self.pipe.set_state(Gst.State.PLAYING)

  def create_pipeline(self):
    #self.pipe = Gst.parse_launch ("v4l2src ! clockoverlay ! capsfilter name=caps ! vp8enc end-usage=cbr resize-allowed=true name=enc target-bitrate=500000 deadline=200000 threads=1 cpu-used=16 ! vp8parse ! tee name=t ! queue ! vp8dec ! autovideosink sync=false t. ! queue ! webmmux ! filesink location=/tmp/test.webm")
    self.pipe = Gst.parse_launch ("v4l2src ! videoscale ! clockoverlay ! capsfilter name=caps ! vp8enc end-usage=cbr resize-allowed=true name=enc target-bitrate=500000 deadline=200000 threads=1 cpu-used=16 ! vp8parse ! vp8dec ! autovideosink sync=false ")
    bus = self.pipe.get_bus()
    bus.add_watch(GLib.PRIORITY_DEFAULT, bus_callback, None)

    self.capsfilter = self.pipe.get_by_name ("caps")
    caps = Gst.caps_from_string ("video/x-raw,height=(int)720")
    self.capsfilter.set_property("caps", caps)
    self.encoder = self.pipe.get_by_name ("enc")

  def on_caps_change_clicked(self, widget):
    width = int(self.width_spin.get_value())
    height = int (self.height_spin.get_value())

    caps = Gst.caps_from_string ("video/x-raw,width=(int)" + str(width) + ",height=(int)" + str(height))
    print ("setting caps to: " + str(width) + ", " + str(height))
    self.capsfilter.set_property("caps", caps)

  def on_change_bitrate_clicked(self, widget):
    bitrate = int (self.bitrate_spin.get_value())
    self.encoder.set_property ("target-bitrate", bitrate)


def main(argv):
  Gst.init(argv)

  win = MyWindow()
  win.connect("delete-event", Gtk.main_quit)
  win.show_all()

  try:
    Gtk.main()
  except:
    pass

  win.pipe.set_state(Gst.State.NULL)


if __name__ == "__main__":
  main(sys.argv)
    #self.pipe = Gst.parse_launch ("videotestsrc is-live=true ! capsfilter name=caps ! x264enc speed-preset=superfast ! h264parse ! decodebin ! autovideosink")
