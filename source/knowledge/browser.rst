===============
Browser Logging
===============

Firefox

https://developer.mozilla.org/en-US/docs/Mozilla/Debugging/HTTP_logging
https://developer.mozilla.org/en-US/docs/Mozilla/Command_Line_Options
https://developer.mozilla.org/en-US/docs/Mozilla/Developer_guide/Gecko_Logging
https://wiki.mozilla.org/Media/WebRTC/Logging

Run with a clean profile:
/usr/bin/firefox -no-remote -profile "$(mktemp --directory)"

Other options:
-url <URL>  Open URL in a new tab or window
-jsconsole  Start application with the Error Console, or, in Firefox, the Browser Console [1].
            [1]: https://developer.mozilla.org/en-US/docs/Tools/Browser_Console

Log ICE/STUN/TURN:
export R_LOG_DESTINATION=stderr
export R_LOG_LEVEL=7
export R_LOG_VERBOSE=1
/usr/bin/firefox -no-remote -profile "$(mktemp --directory)" \
    -url "https://ec2-18-191-95-181.us-east-2.compute.amazonaws.com:8443/"

Debug logging:

In Firefox 54 and later, you can use about:networking, and select the Logging option, to change MOZ_LOG/MOZ_LOG_FILE options on the fly -- without restarting the browser. Also, you can use about:config and set any log option by adding a "logging.xxxxx" variable (right-click -> New), and set it to an integer value of 0-5.

With environment variables:

Linux:
    export MOZ_LOG=timestamp,rotate:200,nsHttp:5,cache2:5,nsSocketTransport:5,nsHostResolver:5
    export MOZ_LOG_FILE=/tmp/firefox.log
    /usr/bin/firefox

Windows:
    set MOZ_LOG=timestamp,rotate:200,nsHttp:5,cache2:5,nsSocketTransport:5,nsHostResolver:5
    set MOZ_LOG_FILE=%TEMP%\firefox.log
    "C:\Program Files\Mozilla Firefox\firefox.exe"

Mac:
    export MOZ_LOG=timestamp,rotate:200,nsHttp:5,cache2:5,nsSocketTransport:5,nsHostResolver:5
    export MOZ_LOG_FILE=~/Desktop/firefox.log
    /Applications/Firefox.app/Contents/MacOS/firefox-bin

With command line arguments:
    /usr/bin/firefox \
        -MOZ_LOG=timestamp,rotate:200,nsHttp:5,cache2:5,nsSocketTransport:5,nsHostResolver:5 \
        -MOZ_LOG_FILE=/tmp/firefox.log




Safari
To enable the Debug menu in Safari, run this command in a terminal:
    defaults write com.apple.Safari IncludeInternalDebugMenu 1




export R_LOG_DESTINATION=stderr
export R_LOG_LEVEL=7
export R_LOG_VERBOSE=1
/usr/bin/firefox \
    -profile "$(mktemp --directory)" \
    -no-remote \
    "https://localhost:8443/" \
    2>&1 | grep 'prflx'




Chrome
======

# /usr/bin/google-chrome
/usr/bin/chromium-browser \
    --user-data-dir="$(mktemp --directory)" \
    --enable-logging=stderr \
    --vmodule='*/webrtc/*=2,*/libjingle/*=2' \
    --v=0 \
    "https://localhost:8443/" \
    2>&1 | grep 'prflx'


H.264 codec
-----------

Chrome uses OpenH264 (same lib as Firefox uses) for encoding, and FFmpeg (which is already used elsewhere in Chrome) for decoding.
Feature page: https://www.chromestatus.com/feature/6417796455989248
Since Chrome 52.
Bug tracker: https://bugs.chromium.org/p/chromium/issues/detail?id=500605
