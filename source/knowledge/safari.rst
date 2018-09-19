============
Apple Safari
============

There are two main implementations of the Safari browser: the Desktop edition which can be found in Mac OS workstations and laptops, and the iOS edition which comes installed as part of the iOS Operating System in mobile devices such as iPhone or iPad.



Codec issues
============

Safari (both Desktop and iOS editions) includes a half-baked implementation of the WebRTC standard, at the least with regards to the codecs compatibility. The WebRTC specs state that both VP8 and H.264 video codecs MUST be implemented in all WebRTC endpoints [*]_, but Apple added only H.264 support to Safari, thus leaving it open to suffer interoperability issues with other peers. They don't play nice, just avoid using Safari.

.. [*] `RFC 7742, Section 5. Mandatory-to-Implement Video Codec <https://tools.ietf.org/html/rfc7742#section-5>`__:

   | WebRTC Browsers MUST implement the VP8 video codec as described in
   | [`RFC6386 <https://tools.ietf.org/html/rfc6386>`__] and H.264 Constrained Baseline as described in [`H264 <http://www.itu.int/rec/T-REC-H.264>`__].
   |
   | WebRTC Non-Browsers that support transmitting and/or receiving video
   | MUST implement the VP8 video codec as described in [`RFC6386 <https://tools.ietf.org/html/rfc6386>`__] and
   | H.264 Constrained Baseline as described in [`H264 <http://www.itu.int/rec/T-REC-H.264>`__].

In order to ensure compatibility with Safari browsers, also caring to not trigger on-the-fly transcoding between video codecs, it is important to make sure that Kurento has been configured with support for H.264, and it is also important to check that the SDP negotiations are actually choosing this as the preferred codec.



HTML policies for video playback
================================

Until now, this has been the recommended way of inserting a video element in any HTML document:

.. code-block:: html

   <video id="myVideo" autoplay></video>

All Kurento tutorials are written to follow this example. As a general rule, most browsers honor the *autoplay* attribute, *Desktop Safari* included; however, we have recently found that *iOS Safari* is an exception to this rule, because it implements a more restrictive set of rules that must be followed in order to allow playing video from a ``<video>`` HTML tag.

There are two things to consider in order to make an HTML document that is compatible with *iOS Safari*:

1. Don't use the ``autoplay`` attribute. Instead, manually call ``HTMLMediaElement.play()``.
2. Add the *playsinline* attribute.

Sources:

- https://webkit.org/blog/6784/new-video-policies-for-ios/
- https://developer.apple.com/library/content/releasenotes/General/WhatsNewInSafari/Articles/Safari_10_0.html
- https://developer.mozilla.org/en-US/docs/Web/HTML/Element/video



<video autoplay>
----------------

The *autoplay* attribute is honored by all browsers, and it makes the ``<video>`` tag to automatically start playing as soon as the source stream is available. In other words: the underlying method ``video.play()`` gets implicitly called as soon as a source video stream becomes available and is set with ``video.srcObject = stream``:

.. code-block:: html

   <video id="myVideo" autoplay></video>

However, in *iOS Safari* (version >= 10), the *autoplay* attribute is only available for videos that have **no sound**, are **muted**, or have a **disabled audio track**. In any other case, the *autoplay* attribute will be ignored, and the video won't start playing automatically when a new stream is set.

The currently recommended solution for this issue is to avoid using the *autoplay* attribute altogether, for the ``<video>`` tags. Instead, manually call the ``play()`` method as a result of some user interaction. For example, when a user clicks a button. The safest way is to call the ``video.play()`` method from inside a button's ``onclick`` event handler.



<video playsinline>
-------------------

Most browsers assume that a video should be played from inside the specific area that the ``<video>`` element occupies. So, for example, a tag such as this one:

.. code-block:: html

   <video id="myVideo"></video>

will play the video in an area that is 480x360 pixels.

That is not the case for *iOS Safari*: all videos play full screen by default: whenever a video starts playing, the browser will maximize its area to fill all the available space in the screen. This can be avoided by adding the *playsinline* attribute to the ``<video>`` tag:

.. code-block:: html

   <video id="myVideo" playsinline></video>

With this, videos will play in *iOS Safari* as they do in any other browser, effectively as inline videos inside their corresponding area.
