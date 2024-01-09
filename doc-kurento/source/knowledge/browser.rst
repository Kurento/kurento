===============
Browser Details
===============

This page is a compendium of information that can be useful to work with or configure different web browsers, for tasks that are common to WebRTC development.

Example commands are written for a Linux shell, because that's what Kurento developers use in their day to day. But most if not all of these commands should be easily converted for use on Windows or Mac systems.

.. contents:: Table of Contents



Firefox
=======

Security sandboxes
------------------

Firefox has several sandboxes that can affect the logging output. For troubleshooting and development, it is recommended that you learn which sandbox might be getting in the way of the logs you need, and disable it:

For example:

* To get logs from ``MOZ_LOG="signaling:5"``, first set ``security.sandbox.content.level`` to *0*.
* To inspect audio issues, disable the audio sandbox by setting ``media.cubeb.sandbox`` to *false*.



Test instance
-------------

To run a new Firefox instance with a clean profile:

.. code-block:: shell

   /usr/bin/firefox -no-remote -profile "$(mktemp --directory)"

Other options:

* ``-jsconsole``: Start Firefox with the `Browser Console <https://firefox-source-docs.mozilla.org/devtools-user/browser_console/index.html>`__.
* ``[-url] <URL>``: Open URL in a new tab or window.



Debug logging
-------------

Sources:

* https://firefox-source-docs.mozilla.org/xpcom/logging.html
* https://firefox-source-docs.mozilla.org/networking/http/logging.html
* https://wiki.mozilla.org/Firefox/CommandLineOptions
* https://wiki.mozilla.org/Media/WebRTC/Logging

Debug logging can be enabled with the parameters *MOZ_LOG* and *MOZ_LOG_FILE*. These are controlled either with environment variables, or command-line flags.

In Firefox >= 54, you can use ``about:networking``, and select the Logging option, to change *MOZ_LOG* / *MOZ_LOG_FILE* options on the fly (without restarting the browser).

You can also use ``about:config`` and set any log option into the profile preferences, by adding (right-click -> New) a variable named ``logging.<NoduleName>``, and setting it to an integer value of 0-5. For example, setting *logging.foo* to *3* will set the module *foo* to start logging at level 3 ("*Info*").

The special pref *logging.config.LOG_FILE* can be set at runtime to change the log file being output to, and the special booleans *logging.config.sync* and *logging.config.add_timestamp* can be used to control the *sync* and *timestamp* properties:

* **sync**: Print each log synchronously, this is useful to check behavior in real time or get logs immediately before crash.
* **timestamp**: Insert timestamp at start of each log line.

Logging Levels:

* **(0) DISABLED**: Indicates logging is disabled. This should not be used directly in code.
* **(1) ERROR**: An error occurred, generally something you would consider asserting in a debug build.
* **(2) WARNING**: A warning often indicates an unexpected state.
* **(3) INFO**: An informational message, often indicates the current program state. and rare enough to be logged at this level.
* **(4) DEBUG**: A debug message, useful for debugging but too verbose to be turned on normally.
* **(5) VERBOSE**: A message that will be printed a lot, useful for debugging program flow and will probably impact performance.

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



Examples
~~~~~~~~

Linux:

.. code-block:: shell

   export MOZ_LOG=timestamp,rotate:200,nsHttp:5,cache2:5,nsSocketTransport:5,nsHostResolver:5
   export MOZ_LOG_FILE=/tmp/firefox.log

   /usr/bin/firefox

Linux with *MOZ_LOG* passed as command line arguments:

.. code-block:: shell

   /usr/bin/firefox \
       -MOZ_LOG=timestamp,rotate:200,nsHttp:5,cache2:5,nsSocketTransport:5,nsHostResolver:5 \
       -MOZ_LOG_FILE=/tmp/firefox.log

Mac:

.. code-block:: shell

   export MOZ_LOG=timestamp,rotate:200,nsHttp:5,cache2:5,nsSocketTransport:5,nsHostResolver:5
   export MOZ_LOG_FILE=/tmp/firefox.log

   /Applications/Firefox.app/Contents/MacOS/firefox-bin

Windows:

.. code-block:: shell

   set MOZ_LOG=timestamp,rotate:200,nsHttp:5,cache2:5,nsSocketTransport:5,nsHostResolver:5
   set MOZ_LOG_FILE=%TEMP%\firefox.log

   "C:\Program Files\Mozilla Firefox\firefox.exe"

:term:`ICE` candidates / :term:`STUN` / :term:`TURN`:

.. code-block:: shell

   export R_LOG_DESTINATION=stderr
   export R_LOG_LEVEL=7
   export R_LOG_VERBOSE=1

   /usr/bin/firefox -no-remote -profile "$(mktemp --directory)" \
       "https://localhost:8443/"

WebRTC dump example (see https://blog.mozilla.org/webrtc/debugging-encrypted-rtp-is-more-fun-than-it-used-to-be/):

.. code-block:: shell

   export MOZ_LOG=timestamp,signaling:5,jsep:5,RtpLogger:5
   export MOZ_LOG_FILE="$PWD/firefox"

   /usr/bin/firefox -no-remote -profile "$(mktemp --directory)" \
       "https://localhost:8443/"

   grep -E "(RTP_PACKET|RTCP_PACKET)" firefox.*.moz_log \
       | cut -d "|" -f 2 \
       | cut -d " " -f 5- \
       | text2pcap -D -n -l 1 -i 17 -u 1234,1235 -t "%H:%M:%S." - firefox-rtp.pcap

Media decoding (audio sandbox can be enabled or disabled with the user preference ``media.cubeb.sandbox``):

.. code-block:: shell

   export MOZ_LOG=timestamp,sync,MediaPipeline:5,MediaStream:5,MediaStreamTrack:5,webrtc_trace:5

   /usr/bin/firefox -no-remote -profile "$(mktemp --directory)" \
       "https://localhost:8443/"



Safari
======

To enable the Debug menu in Safari, run this command in a terminal:

.. code-block:: shell

   defaults write com.apple.Safari IncludeInternalDebugMenu 1



Chrome
======

Test instance
-------------

To run a new Chrome instance with a clean profile:

.. code-block:: shell

   /usr/bin/google-chrome --user-data-dir="$(mktemp --directory)"



Debug logging
-------------

Sources:

* https://www.chromium.org/for-testers/enable-logging/
* https://www.chromium.org/developers/how-tos/run-chromium-with-flags/
* https://peter.sh/experiments/chromium-command-line-switches/

Debug logging is enabled with ``--enable-logging=stderr --log-level=0``. With that, the maximum log level for all modules is given by ``--v=N`` (with N = 0, 1, 2, etc, higher is more verbose, default 0), and per-module levels can be set with ``--vmodule="<categories>"``.

Log categories:

* WebRTC:

  - ``connection=0,*/webrtc/*=2``: Everything related to the WebRTC stack, excluding continuous stats updates from the ``connection.cc`` module.
  - ``*/media/*=2``: Logs from the user media and device capture.
  - ``tls*=1``: Establishment of SSL/TLS connections.

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



Examples
~~~~~~~~

Linux:

.. code-block:: shell

   #TEST_BROWSER="/usr/bin/chromium"
   TEST_BROWSER="/usr/bin/google-chrome"

   TEST_PROFILE="/tmp/chrome-profile"

   "$TEST_BROWSER" \
       --user-data-dir="$TEST_PROFILE" \
       --no-default-browser-check \
       --use-fake-ui-for-media-stream \
       --use-fake-device-for-media-stream \
       --enable-logging=stderr \
       --log-level=0 \
       --v=0 \
       --vmodule="connection=0,*/webrtc/*=2,*/media/*=2,tls*=1" \
       "https://localhost:8080/"



Packet Loss
-----------

A command line for 3% sent packet loss and 5% received packet loss is:

.. code-block:: shell

   --force-fieldtrials=WebRTCFakeNetworkSendLossPercent/3/WebRTCFakeNetworkReceiveLossPercent/5/



H.264 codec
-----------

Chrome uses OpenH264 (same lib as Firefox uses) for encoding, and FFmpeg (which is already used elsewhere in Chrome) for decoding.

* Feature page: https://chromestatus.com/feature/6417796455989248
* Since Chrome 52.
* Bug tracker: https://bugs.chromium.org/p/chromium/issues/detail?id=500605

Autoplay:

* https://developer.chrome.com/blog/autoplay/#best_practices_for_web_developers
* https://www.chromium.org/audio-video/autoplay/



Command-line
============

Chrome
------

.. code-block:: shell

   export WEB_APP_HOST_PORT="198.51.100.1:8443"

   /usr/bin/google-chrome \
       --user-data-dir="$(mktemp --directory)" \
       --enable-logging=stderr \
       --no-first-run \
       --allow-insecure-localhost \
       --allow-running-insecure-content \
       --disable-web-security \
       --unsafely-treat-insecure-origin-as-secure="https://${WEB_APP_HOST_PORT}" \
       "https://${WEB_APP_HOST_PORT}"


Firefox
-------

.. code-block:: text

   export SERVER_PUBLIC_IP="198.51.100.1"

   /usr/bin/firefox \
       -profile "$(mktemp --directory)" \
       -no-remote \
       "https://${SERVER_PUBLIC_IP}:4443/" \
       "http://${SERVER_PUBLIC_IP}:4200/#/test-sessions"



.. _browser-mtu:

Browser MTU
===========

The default **Maximum Transmission Unit (MTU)** in the official `libwebrtc <https://webrtc.org/>`__ implementation is **1200 Bytes** (`source <https://webrtc.googlesource.com/src/+/refs/branch-heads/6099/media/base/media_constants.cc#17>`__). All browsers base their WebRTC implementation on *libwebrtc*, so this means that all use the same MTU:

* `Firefox <https://hg.mozilla.org/releases/mozilla-release/file/FIREFOX_121_0_RELEASE/third_party/libwebrtc/media/base/media_constants.cc#l17>`__.
* `Chrome <https://source.chromium.org/chromium/chromium/src/+/refs/tags/120.0.6099.129:third_party/webrtc/media/base/media_constants.cc;l=17>`__.
* Safari: No public source code, but Safari uses Webkit, and `Webkit uses libwebrtc <https://webrtcinwebkit.org/webrtc-in-safari-11-and-ios-11/>`__, so probably same MTU as the others.



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

Source: The ``GetMaxDefaultVideoBitrateKbps()`` function in `libwebrtc source code <https://source.chromium.org/chromium/chromium/src/+/refs/tags/120.0.6099.129:third_party/webrtc/video/config/encoder_stream_factory.cc;l=79>`__.

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
