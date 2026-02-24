#!/bin/bash

git clone https://github.com/GStreamer/gst-plugins-rs.git
cd gst-plugins-rs
git checkout 0.12.7
git apply ../debian.diff

cargo build --package gst-plugin-rtp --release
cargo deb --separate-debug-symbols  -v --no-build --package=gst-plugin-rtp