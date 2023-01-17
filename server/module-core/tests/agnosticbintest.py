#!/usr/bin/python3

import sys
import gi

gi.require_version('Gst', '1.0')
from gi.repository import Gst
from gi.repository import GLib

loop = GLib.MainLoop()

def remove_element (videosink):
  pipe = videosink.get_parent()
  if pipe == None:
    return

  sink_pad = videosink.get_static_pad ("sink")
  peer = sink_pad.get_peer()

  if peer != None:
    peer_element = peer.get_parent_element()
    if peer_element != None:
      if peer_element.get_factory().get_name() != "agnosticbin":
        remove_element(peer_element)
      else:
        print ("releasing pad: " + str(peer))
        peer_element.release_request_pad (peer)

  pipe.remove(videosink)
  videosink.set_state(Gst.State.NULL)

def bus_callback(bus, message, not_used):
  t = message.type
  if t == Gst.MessageType.EOS:
    sys.stout.write("End-of-stream\n")
    loop.quit()
  elif t == Gst.MessageType.ERROR:
    err, debug = message.parse_error()
    sys.stderr.write("Error: %s: %s\n" % (err, debug))
    element = message.src
    if element.get_factory().get_name() == "xvimagesink":
      remove_element (element)

  return True

def disconnect_videosink(pipe, name):
  Gst.debug_bin_to_dot_file_with_ts(pipe, Gst.DebugGraphDetails.ALL,
      "removevideosink0")
  videosink = pipe.get_by_name(name)
  if videosink == None:
    return

  remove_element(videosink)
  Gst.debug_bin_to_dot_file_with_ts(pipe, Gst.DebugGraphDetails.ALL,
      "removevideosink1")
  return False

def connect_videosink(pipe, name, timeout):
  Gst.debug_bin_to_dot_file_with_ts(pipe, Gst.DebugGraphDetails.ALL, "videosink0")
  videosink = Gst.ElementFactory.make("xvimagesink", name)
  videosink.set_state(Gst.State.PLAYING)
  pipe.add(videosink)
  agnostic = pipe.get_by_name("agnostic")

  agnostic.link(videosink)
  if timeout != 0:
    GLib.timeout_add_seconds(timeout, disconnect_videosink, pipe, name)

  Gst.debug_bin_to_dot_file_with_ts(pipe, Gst.DebugGraphDetails.ALL, "videosink1")
  return False

def connect_enc_videosink(pipe):
  Gst.debug_bin_to_dot_file_with_ts(pipe, Gst.DebugGraphDetails.ALL, "encvideosink0")
  encoder = Gst.ElementFactory.make("theoradec", None)
  videosink = Gst.ElementFactory.make("xvimagesink", None)
  videosink.set_state(Gst.State.PLAYING)
  encoder.set_state(Gst.State.PLAYING)
  pipe.add(videosink)
  pipe.add(encoder)
  agnostic = pipe.get_by_name("agnostic")

  encoder.link(videosink)
  agnostic.link(encoder)

  Gst.debug_bin_to_dot_file_with_ts(pipe, Gst.DebugGraphDetails.ALL, "encvideosink1")
  return False

def connect_audiosink(pipe):
  Gst.debug_bin_to_dot_file_with_ts(pipe, Gst.DebugGraphDetails.ALL, "audiosink0")
  audiosink = Gst.ElementFactory.make("autoaudiosink", None)
  audiosink.set_state(Gst.State.PLAYING)
  pipe.add(audiosink)
  agnostic = pipe.get_by_name("agnostic")

  agnostic.link(audiosink)

  Gst.debug_bin_to_dot_file_with_ts(pipe, Gst.DebugGraphDetails.ALL, "audiosink1")
  return False

def main(argv):
  Gst.init(argv)

  pipe = Gst.Pipeline()
  bus = pipe.get_bus()
  bus.add_watch(GLib.PRIORITY_DEFAULT, bus_callback, None)

  videotest = Gst.ElementFactory.make("videotestsrc", None)
  encoder = Gst.ElementFactory.make("vp8enc", None)
  agnostic = Gst.ElementFactory.make("agnosticbin", "agnostic")
  decoder = Gst.ElementFactory.make("vp8dec", None)
  videosink2 = Gst.ElementFactory.make("xvimagesink", None)
  videosink = Gst.ElementFactory.make("xvimagesink", "videosink0")

  videotest.set_property("pattern", "ball")
  videotest.set_property("is-live", True)

  pipe.add(videotest)
  pipe.add(encoder)
  pipe.add(agnostic)
  pipe.add(videosink)
  pipe.add(decoder)
  pipe.add(videosink2)

  videotest.link(encoder)
  encoder.link(agnostic)
  agnostic.link(videosink)
  agnostic.link(decoder)
  decoder.link(videosink2)
  pipe.set_state(Gst.State.PLAYING)

  Gst.debug_bin_to_dot_file_with_ts(pipe, Gst.DebugGraphDetails.ALL, "playing")
  GLib.timeout_add_seconds(2, connect_videosink, pipe, "videosink1", 4)
  GLib.timeout_add_seconds(2, connect_enc_videosink, pipe)
  GLib.timeout_add_seconds(4, connect_audiosink, pipe)
  GLib.timeout_add_seconds(10, disconnect_videosink, pipe, "videosink0")
  GLib.timeout_add_seconds(16, connect_videosink, pipe, "videosink3", 0)
  GLib.timeout_add_seconds(18, connect_enc_videosink, pipe)

  try:
    loop.run()
  except:
    pass

  Gst.debug_bin_to_dot_file_with_ts(pipe, Gst.DebugGraphDetails.ALL, "end")
  pipe.set_state(Gst.State.NULL)

if __name__ == "__main__":
  main(sys.argv)
