===============
Browser Details
===============

This page is a compendium of information that can be useful to work with or configure different web browsers, for tasks that are common to WebRTC development.

Example commands are written for a Linux shell, because that's what Kurento developers use in their day to day. But most if not all of these commands should be easily converted for use on Windows or Mac systems.



Firefox
=======

Test instance
-------------

To run a new Firefox instance with a clean profile:

.. code-block:: console

   /usr/bin/firefox -no-remote -profile "$(mktemp --directory)"

Other options:

* ``[-url] <URL>``: Open URL in a new tab or window.
* ``-jsconsole``: Start Firefox with the `Browser Console <https://developer.mozilla.org/en-US/docs/Tools/Browser_Console>`__.



Debug logging
-------------

Sources:

* https://wiki.mozilla.org/Media/WebRTC/Logging
* https://developer.mozilla.org/en-US/docs/Mozilla/Debugging/HTTP_logging
* https://developer.mozilla.org/en-US/docs/Mozilla/Command_Line_Options
* https://developer.mozilla.org/en-US/docs/Mozilla/Developer_guide/Gecko_Logging

Debug logging can be enabled with the parameters *MOZ_LOG* and *MOZ_LOG_FILE*. These are controlled either with environment variables, or command-line flags.

In Firefox 54 and later, you can use ``about:networking``, and select the Logging option, to change *MOZ_LOG* / *MOZ_LOG_FILE* options on the fly (without restarting the browser).

Lastly, you can also use ``about:config`` and set any log option into the profile preferences, by adding (right-click -> New) a variable named ``logging.<NoduleName>``, and setting it to an integer value of 0-5. For example, setting *logging.foo* to *3* will set the module *foo* to start logging at level 3 ("*Info*"). The special pref *logging.config.LOG_FILE* can be set at runtime to change the log file being output to, and the special boolean prefs *logging.config.sync* and *logging.config.add_timestamp* can be used to control the *sync* and *timestamp* properties:

- **sync**: Print each log synchronously, this is useful to check behavior in real time or get logs immediately before crash.
- **timestamp**: Insert timestamp at start of each log line.

These are the Mozilla Logging Levels:

- **(0) DISABLED**: Indicates logging is disabled. This should not be used directly in code.
- **(1) ERROR**: An error occurred, generally something you would consider asserting in a debug build.
- **(2) WARNING**: A warning often indicates an unexpected state.
- **(3) INFO**: An informational message, often indicates the current program state. and rare enough to be logged at this level.
- **(4) DEBUG**: A debug message, useful for debugging but too verbose to be turned on normally.
- **(5) VERBOSE**: A message that will be printed a lot, useful for debugging program flow and will probably impact performance.

Some examples:

Linux:

.. code-block:: console

   export MOZ_LOG=timestamp,rotate:200,nsHttp:5,cache2:5,nsSocketTransport:5,nsHostResolver:5
   export MOZ_LOG_FILE=/tmp/firefox.log
   /usr/bin/firefox

Mac:

.. code-block:: console

   export MOZ_LOG=timestamp,rotate:200,nsHttp:5,cache2:5,nsSocketTransport:5,nsHostResolver:5
   export MOZ_LOG_FILE=~/Desktop/firefox.log
   /Applications/Firefox.app/Contents/MacOS/firefox-bin

Windows:

.. code-block:: console

   set MOZ_LOG=timestamp,rotate:200,nsHttp:5,cache2:5,nsSocketTransport:5,nsHostResolver:5
   set MOZ_LOG_FILE=%TEMP%\firefox.log
   "C:\Program Files\Mozilla Firefox\firefox.exe"

With command line arguments:

.. code-block:: console

   /usr/bin/firefox \
       -MOZ_LOG=timestamp,rotate:200,nsHttp:5,cache2:5,nsSocketTransport:5,nsHostResolver:5 \
       -MOZ_LOG_FILE=/tmp/firefox.log

Log :term:`ICE` candidates / :term:`STUN` / :term:`TURN`:

.. code-block:: console

   export R_LOG_DESTINATION=stderr
   export R_LOG_LEVEL=7
   export R_LOG_VERBOSE=1

   /usr/bin/firefox -no-remote -profile "$(mktemp --directory)" \
       "https://localhost:8443/"

WebRTC dump example (see https://blog.mozilla.org/webrtc/debugging-encrypted-rtp-is-more-fun-than-it-used-to-be/):

.. code-block:: console

   export MOZ_LOG=timestamp,signaling:5,jsep:5,RtpLogger:5
   export MOZ_LOG_FILE="$PWD/firefox"

   /usr/bin/firefox -no-remote -profile "$(mktemp --directory)" \
       "https://localhost:8443/"

   grep -E '(RTP_PACKET|RTCP_PACKET)' firefox.*.moz_log \
       | cut -d '|' -f 2 \
       | cut -d ' ' -f 5- \
       | text2pcap -D -n -l 1 -i 17 -u 1234,1235 -t '%H:%M:%S.' - firefox-rtp.pcap

Other log categories:

Multimedia:

* AudioStream:5
* MediaCapabilities:5
* MediaControl:5
* MediaEncoder:5
* MediaManager:5
* MediaRecorder:5
* MediaStream:5
* MediaStreamTrack:5
* MediaTimer:5
* MediaTrackGraph:5
* Muxer:5
* PlatformDecoderModule:5
* PlatformEncoderModule:5
* TrackEncoder:5
* VP8TrackEncoder:5
* VideoEngine:5
* VideoFrameConverter:5
* cubeb:5

WebRTC:

* Autoplay:5
* GetUserMedia:5
* webrtc_trace:5
* signaling:5
* MediaPipeline:5
* RtpLogger:5
* RTCRtpReceiver:5
* sdp:5

Notes:

* The audio sandbox can be enabled or disabled with the user preference *media.cubeb.sandbox*.

.. code-block:: console

   export MOZ_LOG=timestamp,sync,MediaPipeline:5,MediaStream:5,MediaStreamTrack:5,webrtc_trace:5

   /usr/bin/firefox -no-remote -profile "$(mktemp --directory)" \
       "https://localhost:8443/"

   # Equivalent code for Selenium:
   firefoxOptions.addPreference("media.cubeb.sandbox", true);
   firefoxOptions.addPreference("logging.config.add_timestamp", true);
   firefoxOptions.addPreference("logging.config.sync", true);
   firefoxOptions.addPreference("logging.cubeb", 5);
   firefoxOptions.addPreference("logging.MediaTrackGraph", 5);



Safari
======

To enable the Debug menu in Safari, run this command in a terminal:

.. code-block:: console

   defaults write com.apple.Safari IncludeInternalDebugMenu 1



Chrome
======

Test instance
-------------

To run a new Chrome instance with a clean profile:

.. code-block:: console

   /usr/bin/google-chrome --user-data-dir="$(mktemp --directory)"



Debug logging
-------------

Sources:

* https://webrtc.org/web-apis/chrome/
* https://www.chromium.org/for-testers/enable-logging


.. code-block:: console

   /usr/bin/google-chrome --user-data-dir="$(mktemp --directory)" \
       --enable-logging=stderr \
       --log-level=0 \
       --v=0 \
       --vmodule='*/webrtc/*=2,*/libjingle/*=2,*=-2' \
       "https://localhost:8443/"

Other options:

.. code-block:: console

   --use-fake-device-for-media-stream \
   --use-file-for-fake-audio-capture="${HOME}/test.wav" \


H.264 codec
-----------

Chrome uses OpenH264 (same lib as Firefox uses) for encoding, and FFmpeg (which is already used elsewhere in Chrome) for decoding.
Feature page: https://www.chromestatus.com/feature/6417796455989248
Since Chrome 52.
Bug tracker: https://bugs.chromium.org/p/chromium/issues/detail?id=500605

Autoplay:
- https://developers.google.com/web/updates/2017/09/autoplay-policy-changes#best-practices
- https://www.chromium.org/audio-video/autoplay



H.264 encoding/decoding profile
===============================

By default, Chrome uses this line in the SDP Offer for an H.264 media:

.. code-block:: text

   a=fmtp:100 level-asymmetry-allowed=1;packetization-mode=1;profile-level-id=42e01f

`profile-level-id` is an SDP attribute, defined in [RFC 6184] as the hexadecimal representation of the *Sequence Parameter Set* (SPS) from the H.264 Specification. The value **42e01f** decomposes as the following parameters:
- `profile_idc` = 0x42 = 66
- `profile-iop` = 0xE0 = 1110_0000
- `level_idc` = 0x1F = 31

:rfc:`6184`.

These values translate into the **Constrained Baseline Profile, Level 3.1**.



Command-line
============

Chrome
------

.. code-block:: console

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



WebRTC JavaScript API
=====================

Generate an SDP Offer.

.. code-block:: text

   let pc1 = new RTCPeerConnection();
   navigator.mediaDevices.getUserMedia({ video: true, audio: true })
   .then((stream) => {
       stream.getTracks().forEach((track) => {
           console.log("Local track available: " + track.kind);
           pc1.addTrack(track, stream);
       });
       pc1.createOffer().then((offer) => {
           console.log(JSON.stringify(offer).replace(/\\r\\n/g, '\n'));
       });
   });



.. _browser-mtu:

Browser MTU
===========

The default **Maximum Transmission Unit (MTU)** in the official `libwebrtc <https://webrtc.org/>`__ implementation is **1200 Bytes** (`source code <https://webrtc.googlesource.com/src/+/d82a02c837d33cdfd75121e40dcccd32515e42d6/media/engine/constants.cc#15>`__). All browsers base their WebRTC implementation on *libwebrtc*, so this means that all use the same MTU:

* `Chrome source code <https://codesearch.chromium.org/chromium/src/third_party/webrtc/media/engine/constants.cc?rcl=f092e4d0ff252f52404a0c867f20cf103bbaa663&l=15>`__.
* `Firefox source code <https://dxr.mozilla.org/mozilla-central/rev/4c982daa151954c59f20a9b9ac805c1768a350c2/media/webrtc/trunk/webrtc/media/engine/constants.cc#16>`__.
* Safari: No public source code, but Safari uses Webkit, and `Webkit uses libwebrtc <https://www.webrtcinwebkit.org/blog/2017/7/2/webrtc-in-safari-11-and-ios-11>`__, so probably same MTU as the others.



Initial bandwidth estimation
============================

WebRTC **bandwidth estimation (BWE)** was implemented first with *Google REMB*, and later with *Transport-CC*. Clients need to start "somewhere" with their estimations, and the official `libwebrtc <https://webrtc.org/>`__ implementation chose to do so at 300 kbps (kilobits per second) (`source code <https://webrtc.googlesource.com/src/+/d82a02c837d33cdfd75121e40dcccd32515e42d6/api/transport/bitrate_settings.h#45>`__). All browsers base their WebRTC implementation on *libwebrtc*, so this means that all use the same initial BWE:

* `Chrome source code <https://codesearch.chromium.org/chromium/src/third_party/webrtc/api/transport/bitrate_settings.h?rcl=f092e4d0ff252f52404a0c867f20cf103bbaa663&l=45>`__.
* `Firefox source code <https://dxr.mozilla.org/mozilla-central/rev/4c982daa151954c59f20a9b9ac805c1768a350c2/media/webrtc/trunk/webrtc/call/call.h#84>`__.
