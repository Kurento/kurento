============
Apple Safari
============

There are two main implementations of the Safari browser: the Desktop edition which can be found in Mac OS workstations and laptops, and the iOS edition which comes installed as part of the iOS Operating System in mobile devices such as iPhone or iPad.



Codec issues
============

Safari (both Desktop and iOS editions) included a half-baked implementation of the WebRTC standard, at the least with regards to the codecs compatibility. The WebRTC specs state that both VP8 and H.264 video codecs MUST be implemented in all WebRTC endpoints [*]_, but Apple only added VP8 support starting from `Safari Release 68 <https://developer.apple.com/safari/technology-preview/release-notes/#r68>`__. Older versions of the browser won't be able to decode VP8 video, so if the source video isn't already in H.264 format, Kurento Media Server will need to transcode the input video so they can be received by Safari.

.. [*] `RFC 7742, Section 5. Mandatory-to-Implement Video Codec <https://tools.ietf.org/html/rfc7742#section-5>`__:
   | WebRTC Browsers MUST implement the VP8 video codec as described in
   | [`RFC6386 <https://tools.ietf.org/html/rfc6386>`__] and H.264 Constrained Baseline as described in [`H264 <http://www.itu.int/rec/T-REC-H.264>`__].
   |
   | WebRTC Non-Browsers that support transmitting and/or receiving video
   | MUST implement the VP8 video codec as described in [`RFC6386 <https://tools.ietf.org/html/rfc6386>`__] and
   | H.264 Constrained Baseline as described in [`H264 <http://www.itu.int/rec/T-REC-H.264>`__].

In order to ensure compatibility with Safari browsers, also caring to not trigger on-the-fly transcoding between video codecs, it is important to make sure that Kurento has been configured with support for H.264, and it is also important to check that the SDP negotiations are actually choosing this as the preferred codec.

If you are targeting Safari version 68+, then this won't pose any problem, as now both H.264 and VP8 can be used for WebRTC.



HTML policies for video playback
================================

Until recently, this has been the recommended way of inserting a video element in any HTML document:

.. code-block:: html

   <video id="myVideo" autoplay></video>

All Kurento tutorials are written to follow this example. As a general rule, most browsers honor the *autoplay* attribute, *Desktop Safari* included; however, *iOS Safari* is an exception to this rule, because it implements a more restrictive set of rules that must be followed in order to allow playing video from a ``<video>`` HTML tag.

You should make a couple changes in order to follow with all the latest changes in browser's policies for automatically playing videos:

1. Start automatic video playback without audio, using the *muted* attribute together with *autoplay*.
2. Add the *playsinline* attribute if you want to avoid fullscreen videos in *iOS Safari*.

A video tag that includes all these suggestions would be like this:

.. code-block:: html

   <video id="myVideo" playsinline autoplay muted></video>

Sources for this section:

- https://webkit.org/blog/6784/new-video-policies-for-ios/
- https://developer.mozilla.org/en-US/docs/Web/HTML/Element/video#Browser_compatibility
- https://developer.apple.com/library/content/releasenotes/General/WhatsNewInSafari/Articles/Safari_10_0.html



autoplay muted
--------------

The *autoplay* attribute is honored by all browsers, and it makes the ``<video>`` tag to automatically start playing as soon as the source stream is available. In other words: the method ``video.play()`` gets implicitly called as soon as a source video stream becomes available and is set with ``video.srcObject = stream``.

However, in *iOS Safari* (version >= 10), the *autoplay* attribute is only available for videos that **have no sound**, are **muted**, or have a **disabled audio track**. In any other case, the *autoplay* attribute will be ignored, and the video won't start playing automatically when a new stream is set.

The solution that is most intuitive for the user is that a muted video is presented, and then the user is asked to click somewhere in order to enable the audio:

.. code-block:: html

   <video id="myVideo" autoplay muted></video>

This will allow the user interface to at least automatically start playing a video, so the user will see some movement and acknowledge that the media playback has started. Then, an optional label might ask the user to press to unmute, an action that would comply with the browser's *autoplay* policies.

Another alternative is to avoid using the *autoplay* attribute altogether. Instead, manually call the ``play()`` method as a result of some user interaction. The safest way is to call the ``myVideo.play()`` method from inside a button's *onclick* event handler.



playsinline
-----------

Most browsers assume that a video should be played from inside the specific area that the ``<video>`` element occupies. So, for example, a tag such as this one:

.. code-block:: html

   <video id="myVideo" width="480px" height="360px"></video>

will play the video in an area that is 480x360 pixels.

That is not the case for *iOS Safari*, where all videos play full screen by default: whenever a video starts playing, the browser will maximize its area to fill all the available space in the screen. This can be avoided by adding the *playsinline* attribute to the ``<video>`` tag:

.. code-block:: html

   <video id="myVideo" width="480px" height="360px" playsinline></video>

With this, videos will play in *iOS Safari* as they do in any other browser, as inline videos inside their corresponding area.
