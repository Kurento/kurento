===============
Browser Details
===============

This page is a compendium of information that can be useful to work with or configure different web browsers, for tasks that are common to WebRTC development.

Example commands are written for a Linux shell, because that's what Kurento developers use in their day to day. But most if not all of these commands should be easily converted for use on Windows or Mac systems.

.. contents:: Table of Contents



Firefox
=======

Quickstart commands
-------------------

**Basic command**

Runs a new Firefox instance with a clean profile:

.. code-block:: shell

   /usr/bin/firefox \
       --no-remote \
       --profile "$(mktemp --directory)"

**Extended command for WebRTC testing**

Requires to first write some useful settings in form of an initial ``user.js`` file:

.. code-block:: shell

   PROFILE_DIR="$(mktemp --directory)"

   tee "$PROFILE_DIR/user.js" >/dev/null <<'EOF'
   // Disable first-start screens.
   user_pref("browser.aboutwelcome.enabled", false);
   user_pref("browser.newtabpage.activity-stream.feeds.topsites", false);
   user_pref("browser.newtabpage.activity-stream.showSponsoredTopSites", false);
   user_pref("datareporting.policy.firstRunURL", "");
   user_pref("permissions.default.camera", 1);
   user_pref("permissions.default.microphone", 1);
   //
   // Mozilla prefs for testing. Taken directly from Mozilla source code:
   // https://searchfox.org/mozilla-central/source/testing/profiles/web-platform/user.js
   //
   // Don't use the new tab page but about:blank for opened tabs
   user_pref("browser.newtabpage.enabled", false);
   // Disable session restore infobar.
   user_pref("browser.startup.couldRestoreSession.count", -1);
   // Don't show the Bookmarks Toolbar on any tab
   user_pref("browser.toolbars.bookmarks.visibility", "never");
   // Expose TestUtils interface
   user_pref("dom.testing.testutils.enabled", true);
   // Enable fake media streams for getUserMedia
   user_pref("media.navigator.streams.fake", true);
   // Disable permission prompt for getUserMedia
   user_pref("media.navigator.permission.disabled", true);
   // Enable direct connection
   user_pref("network.proxy.type", 0);
   // Run the font loader task eagerly for more predictable behavior
   user_pref("gfx.font_loader.delay", 0);
   // Disable safebrowsing components
   user_pref("browser.safebrowsing.update.enabled", false);
   // Turn off update
   user_pref("app.update.disabledForTesting", true);
   EOF

And then launch a standalone Firefox instance that points to that directory:

.. code-block:: shell

   /usr/bin/firefox \
       --no-remote \
       --profile "$PROFILE_DIR"



Security sandboxes
------------------

Firefox has several sandboxes that can affect the logging output. For troubleshooting and development, it is recommended that you learn which sandbox might be getting in the way of the logs you need, and disable it:

For example:

* To get logs from ``MOZ_LOG="signaling:5"``, first set ``security.sandbox.content.level`` to *0*.
* To inspect audio issues, disable the audio sandbox by setting ``media.cubeb.sandbox`` to *false*.



Debug logging
-------------

Sources:

* https://firefox-source-docs.mozilla.org/xpcom/logging.html
* https://firefox-source-docs.mozilla.org/networking/http/logging.html
* https://wiki.mozilla.org/Firefox/CommandLineOptions
* https://wiki.mozilla.org/Media/WebRTC/Logging

Debug logging can be enabled with the parameters *MOZ_LOG* and *MOZ_LOG_FILE*. These are controlled either with environment variables, or command-line flags.

In Firefox >= 54, you can use ``about:networking``, and select the Logging option, to change *MOZ_LOG* / *MOZ_LOG_FILE* options without restarting the browser.

You can also use ``about:config`` and set any log option into the profile preferences, by adding (right-click -> New) a variable named ``logging.<NoduleName>``, and setting it to an integer value of 0-5. For example, setting *logging.foo* to *3* will set the module *foo* to start logging at level 3 ("*Info*").

The special pref *logging.config.LOG_FILE* can be set at runtime to change the log file being output to, and the special booleans *logging.config.sync* and *logging.config.add_timestamp* can be used to control the *sync* and *timestamp* properties:

* **sync**: print each log synchronously, this is useful to check behavior in real time or get logs immediately before crash.
* **timestamp**: insert timestamp at start of each log line.

Logging Levels:

* **(0) DISABLED**: indicates logging is disabled. This should not be used directly in code.
* **(1) ERROR**: an error occurred, generally something you would consider asserting in a debug build.
* **(2) WARNING**: a warning often indicates an unexpected state.
* **(3) INFO**: an informational message, often indicates the current program state. and rare enough to be logged at this level.
* **(4) DEBUG**: a debug message, useful for debugging but too verbose to be turned on normally.
* **(5) VERBOSE**: a message that will be printed a lot, useful for debugging program flow and will probably impact performance.

Log categories:

* Multimedia:

  - AudioStream:5
  - MediaCapabilities:5
  - MediaControl:5
  - MediaEncoder:5
  - MediaManager:5
  - MediaRecorder:5
  - MediaStream:5
  - MediaStreamTrack:5
  - MediaTimer:5
  - MediaTrackGraph:5
  - Muxer:5
  - PlatformDecoderModule:5
  - PlatformEncoderModule:5
  - TrackEncoder:5
  - VP8TrackEncoder:5
  - VideoEngine:5
  - VideoFrameConverter:5
  - cubeb:5

* WebRTC:

  - Autoplay:5
  - GetUserMedia:5
  - webrtc_trace:5
  - signaling:5
  - MediaPipeline:5
  - RtpLogger:5
  - RTCRtpReceiver:5
  - sdp:5



Debug logging examples
~~~~~~~~~~~~~~~~~~~~~~

General logging of various modules:

.. code-block:: shell

   export MOZ_LOG="timestamp,rotate:200,nsHttp:5,cache2:5,nsSocketTransport:5,nsHostResolver:5"
   export MOZ_LOG_FILE="/tmp/firefox.log"

:term:`ICE` candidates / :term:`STUN` / :term:`TURN`:

.. code-block:: shell

   export R_LOG_DESTINATION=stderr
   export R_LOG_LEVEL=7
   export R_LOG_VERBOSE=1

WebRTC dump example (see https://blog.mozilla.org/webrtc/debugging-encrypted-rtp-is-more-fun-than-it-used-to-be/):

.. code-block:: shell

   export MOZ_LOG="timestamp,signaling:5,jsep:5,RtpLogger:5"
   export MOZ_LOG_FILE="/tmp/firefox"

   # Later, the resulting logs can be converted into Packet Capture files:
   grep -E "(RTP_PACKET|RTCP_PACKET)" firefox.*.moz_log \
       | cut -d "|" -f 2 \
       | cut -d " " -f 5- \
       | text2pcap -D -n -l 1 -i 17 -u 1234,1235 -t "%H:%M:%S." - firefox-rtp.pcap

Media decoding (audio sandbox can be enabled or disabled with the user preference ``media.cubeb.sandbox``):

.. code-block:: shell

   export MOZ_LOG="timestamp,sync,MediaPipeline:5,MediaStream:5,MediaStreamTrack:5,webrtc_trace:5"



Chrome
======

Quickstart commands
-------------------

**Basic command**

Runs a new Chrome instance with a clean profile:

.. code-block:: shell

   # Depending on your system, you'll want to use either of these:
   # /usr/bin/chromium
   # /usr/bin/chromium-browser
   # /usr/bin/google-chrome

   /usr/bin/chromium \
       --user-data-dir="$(mktemp --directory)"

**Extended command for WebRTC testing**

.. code-block:: shell

   /usr/bin/chromium \
       --user-data-dir="$(mktemp --directory)" \
       --guest \
       --no-default-browser-check \
       --auto-accept-camera-and-microphone-capture \
       --use-fake-device-for-media-stream \
       --enable-logging=stderr \
       --log-level=0 \
       --v=0 \
       --vmodule="basic_ice_controller=0,connection=0,encoder_bitrate_adjuster=0,goog_cc_network_control=0,pacing_controller=0,video_stream_encoder=0,*/webrtc/*=2,*/media/*=2,tls*=1" \
       "https://localhost:8080/"

Notes:

* ``--guest``: activate "browse without sign-in" (guest session) mode, disabling extensions, sync, bookmarks, and password manager pop-ups.

``--no-default-browser-check``: disable "set as default browser" prompt.

* ``--auto-accept-camera-and-microphone-capture``: automatically accept all requests to access the camera and microphone.

  This flag deprecates the older ``--use-fake-ui-for-media-stream``, which had a negative effect on screen/tab capture.

* ``--use-fake-device-for-media-stream``: use synthetic audio and video media to simulate capture devices (camera, microphone, etc).

  Alternatively, a local file can be provided to be used instead:

  - ``--use-file-for-fake-audio-capture="/path/to/file.wav"``: use a WAV file as the audio source.

  - ``--use-file-for-fake-video-capture="/path/to/file.y4m"``: use a YUV4MPEG2 (Y4M) or MJPEG file as the video source. `More <https://source.chromium.org/chromium/chromium/src/+/refs/tags/120.0.6099.129:media/capture/video/file_video_capture_device.h;l=25-35>`__ `details <https://source.chromium.org/chromium/chromium/src/+/refs/tags/120.0.6099.129:media/capture/video/file_video_capture_device.cc;l=70-75>`__:

    - Y4M videos should have *.y4m* file extension and MJPEG videos should have *.mjpeg* file extension.
    - Only interlaced I420 pixel format is supported.
    - Example Y4M videos can be found here: https://media.xiph.org/video/derf/
    - Example MJPEG videos can be found here: https://chromium.googlesource.com/chromium/src/+/refs/tags/120.0.6099.129/media/test/data

* ``--unsafely-treat-insecure-origin-as-secure="URL,..."``: allow insecure origins to use features that would require a `Secure Context <https://www.w3.org/TR/secure-contexts/>`__ (such as ``getUserMedia()``, WebRTC, etc.) when served from localhost or over HTTP.

  A better approach is to serve the origins over HTTPS, but this flag can be useful for one-off testing.



Debug logging
-------------

Sources:

* https://www.chromium.org/for-testers/enable-logging/
* https://www.chromium.org/developers/how-tos/run-chromium-with-flags/
* https://peter.sh/experiments/chromium-command-line-switches/

Debug logging is enabled with ``--enable-logging=stderr --log-level=0``. With that, the maximum log level for all modules is given by ``--v=N`` (with N = 0, 1, 2, etc, higher is more verbose, default 0), and per-module levels can be set with ``--vmodule="<categories>"``.

Log categories:

* WebRTC:

  - ``*/webrtc/*=2``: everything related to the WebRTC stack.

    It's strongly suggested to disable some modules that would otherwise flood the logs:

    - ``basic_ice_controller=0``
    - ``connection=0``
    - ``encoder_bitrate_adjuster=0``
    - ``goog_cc_network_control=0``
    - ``pacing_controller=0``
    - ``video_stream_encoder=0``

  - ``*/media/*=2``: logs from the user media and device capture.

  - ``tls*=1``: establishment of SSL/TLS connections.

  See below for a full example command that can be copy-pasted.

How to find the module names for ``--vmodule``:

* Run with a very verbose general logging level, such as ``--v=9``.

* Start with ``--vmodule="compositor=0,display=0,layer_tree_*=0,segment_*=0,*/metrics/*=0"`` (these are very noisy modules that would otherwise flood the log).

* Search the log for the lines you are interested in. For example:

  .. code-block:: text

     [VERBOSE2:video_capture_metrics.cc(158)] Device supports PIXEL_FORMAT_I420 at 96x96 (0)

* Open the Google Chromium code search page: https://source.chromium.org/chromium/chromium/src

* Search for the desired module name. In the example, this search term would match exactly:

  .. code-block:: text

     file:video_capture_metrics.cc content:"Device supports"

  Take note of the module path: ``media/capture/video/video_capture_metrics.cc``.

* Add either the module name or path with wildcards to the ``--vmodule`` list. In the example, any of these would enable the given log message:

  .. code-block:: shell

     --vmodule="video_capture_metrics=2"
     --vmodule="video_capture*=2"
     --vmodule="*/media/*=2"



Packet Loss
-----------

A command line for 3% sent packet loss and 5% received packet loss is:

.. code-block:: shell

   --force-fieldtrials="WebRTCFakeNetworkSendLossPercent/3/WebRTCFakeNetworkReceiveLossPercent/5/"



H.264 codec
-----------

Chrome uses OpenH264 (same lib as Firefox uses) for encoding, and FFmpeg (which is already used elsewhere in Chrome) for decoding.

* Feature page: https://chromestatus.com/feature/6417796455989248
* Since Chrome 52.
* Bug tracker: https://bugs.chromium.org/p/chromium/issues/detail?id=500605

Autoplay:

* https://developer.chrome.com/blog/autoplay/#best_practices_for_web_developers
* https://www.chromium.org/audio-video/autoplay/



Safari
======

To enable the Debug menu in Safari, run this command in a terminal:

.. code-block:: shell

   defaults write com.apple.Safari IncludeInternalDebugMenu 1



.. _browser-mtu:

Browser MTU
===========

The default **Maximum Transmission Unit (MTU)** in the official `libwebrtc <https://webrtc.org/>`__ implementation is **1200 Bytes** (`source <https://webrtc.googlesource.com/src/+/refs/branch-heads/6099/media/base/media_constants.cc#17>`__). All browsers base their WebRTC implementation on *libwebrtc*, so this means that all use the same MTU:

* `Firefox <https://hg.mozilla.org/releases/mozilla-release/file/FIREFOX_121_0_RELEASE/third_party/libwebrtc/media/base/media_constants.cc#l17>`__.
* `Chrome <https://source.chromium.org/chromium/chromium/src/+/refs/tags/120.0.6099.129:third_party/webrtc/media/base/media_constants.cc;l=17>`__.
* Safari: no public source code, but Safari uses Webkit, and `Webkit uses libwebrtc <https://webrtcinwebkit.org/webrtc-in-safari-11-and-ios-11/>`__, so probably same MTU as the others.



Bandwidth Estimation
====================

WebRTC **bandwidth estimation (BWE)** was implemented first with *Google REMB*, and later with *Transport-CC*. Clients need to start "somewhere" with their estimations, and the official `libwebrtc <https://webrtc.org/>`__ implementation chose to do so at 300 kbps (kilobits per second) (`source <https://webrtc.googlesource.com/src/+/refs/branch-heads/6099/api/transport/bitrate_settings.h#45>`__). All browsers base their WebRTC implementation on *libwebrtc*, so this means that all use the same initial BWE:

* `Firefox <https://hg.mozilla.org/releases/mozilla-release/file/FIREFOX_121_0_RELEASE/third_party/libwebrtc/api/transport/bitrate_settings.h#l45>`__.
* `Chrome <https://source.chromium.org/chromium/chromium/src/+/refs/tags/120.0.6099.129:third_party/webrtc/api/transport/bitrate_settings.h;l=45>`__.



.. _browser-video:

Video Encoding
==============

Video Bitrate
-------------

Web browsers will try to estimate the real performance of the network, and with this information they adapt their video output quality. Most browsers are able to adjust the **video bitrate**; in addition, Chrome also dynamically adapts the **resolution** and **framerate** of its video output.

The **maximum video bitrate** is calculated for WebRTC by following a simple rule based on the dimensions of the video source:

* 600 kbps if ``width * height <= 320 * 240``.
* 1700 kbps if ``width * height <= 640 * 480``.
* 2000 kbps (2 Mbps) if ``width * height <= 960 * 540``.
* 2500 kbps (2.5 Mbps) for bigger video sizes.
* Never less than 1200 kbps, if the video is a screen capture.

Source: the ``GetMaxDefaultVideoBitrateKbps()`` function in `libwebrtc source code <https://source.chromium.org/chromium/chromium/src/+/refs/tags/120.0.6099.129:third_party/webrtc/video/config/encoder_stream_factory.cc;l=79>`__.

To verify what is exactly being sent by your web browser, check its internal WebRTC stats. For example, to check the outbound stats in Chrome:

#. Open this URL: ``chrome://webrtc-internals/``.
#. Look for the stat name "*Stats graphs for RTCOutboundRTPVideoStream (outbound-rtp)*".
#. You will find the effective output bitrate in ``[bytesSent_in_bits/s]``, and the output resolution in ``frameWidth`` and ``frameHeight``.

You can also check what is the network bandwidth estimation in Chrome:

#. Look for the stat name "*Stats graphs for RTCIceCandidatePair (candidate-pair)*". Note that there might be several of these, but only one will be active.
#. Find the output network bandwidth estimation in ``availableOutgoingBitrate``. Chrome will try to slowly increase its effective output bitrate, until it reaches this estimation.



H.264 profile
-------------

By default, Chrome uses this line in the SDP Offer for an H.264 media:

.. code-block:: text

   a=fmtp:100 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42e01f

`profile-level-id` is an SDP attribute, defined in :rfc:`6184` as the hexadecimal representation of the *Sequence Parameter Set* (SPS) from the H.264 Specification. The value **42e01f** decomposes as the following parameters:

* `profile_idc` = 0x42 = 66
* `profile-iop` = 0xE0 = 1110_0000
* `level_idc` = 0x1F = 31

These values translate into the **Constrained Baseline Profile, Level 3.1**.



Source Code URLs
================

Here is where you can find URLs to the different web browser source code repositories. Also, for linking to specific lines of code, it's always a good idea to use permalinks such that future visitors find the exact same source code that was linked, and not a newer version of it which might have changed.

**Firefox**:

* Code search: https://searchfox.org/mozilla-central/source/
* Code repository (development): https://hg.mozilla.org/mozilla-central/
* Code repository (release): https://hg.mozilla.org/releases/mozilla-release/
* List of tagged releases: https://hg.mozilla.org/releases/mozilla-release/tags

* Sample permalink to a specific line of code in Firefox v121.0:

  .. code-block:: text

     https://hg.mozilla.org/releases/mozilla-release/file/FIREFOX_121_0_RELEASE/path/to/file#l123

**Chrome**:

* Code search: https://source.chromium.org/chromium/chromium/src
* Code repository: https://chromium.googlesource.com/chromium/src/
* List of tagged releases: https://chromium.googlesource.com/chromium/src/+refs

* Sample permalink to a specific line of code in Chrome v120.0.6099.129:

  .. code-block:: text

     https://source.chromium.org/chromium/chromium/src/+/refs/tags/120.0.6099.129:path/to/file;l=123

**WebRTC**:

* Code search: -
* Code repository: https://webrtc.googlesource.com/src/
* List of tagged releases: https://chromiumdash.appspot.com/branches

* Sample permalink to a specific line of code in WebRTC M120:

  .. code-block:: text

     https://webrtc.googlesource.com/src/+/refs/branch-heads/6099/path/to/file#123
