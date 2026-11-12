================================
Recording: containers and codecs
================================

The **RecorderEndpoint** (API docs: `Java <../_static/client-javadoc/org/kurento/client/RecorderEndpoint.html>`__, `JavaScript <../_static/client-jsdoc/module-elements.RecorderEndpoint.html>`__) stores the media that it receives into a file. Two things determine what ends up inside that file:

* The **container** (MP4, MKV, WEBM, ...), chosen with the *mediaProfile* constructor parameter (``MediaProfileSpecType``).
* The **video codec** (H.264, H.265, VP8, VP9, AV1), chosen with the optional *videoCodec* constructor parameter (``RecorderVideoCodec``). Its default value is ``DEFAULT``, which means "use the default behavior of the server", currently the default codec of the container.

.. contents:: Table of Contents



Default behavior
================

If *videoCodec* is not provided (or it is ``DEFAULT``), the server uses its default behavior, which currently is the default codec of each container, exactly as in previous versions of Kurento:

======================  =============
Container               Default codec
======================  =============
``MP4``                 H.264
``MKV``                 H.264
``WEBM``                VP8
======================  =============

Audio is not configurable: it is stored as AAC in MP4 and MKV, and as Opus in WEBM.



Valid combinations
==================

======================  =====  =====  =====  =====  =====
Container               H264   H265   VP8    VP9    AV1
======================  =====  =====  =====  =====  =====
``MP4``                 yes    yes    no     yes    yes
``MKV``                 yes    yes    yes    yes    yes
``WEBM``                no     no     yes    yes    yes
======================  =====  =====  =====  =====  =====

* The ``_VIDEO_ONLY`` variants of each profile (e.g. ``MP4_VIDEO_ONLY``) follow the same rules as their full counterparts.
* The ``_AUDIO_ONLY`` variants do not store any video, so they ignore *videoCodec*.
* ``FLV``, ``JPEG_VIDEO_ONLY`` and ``KURENTO_SPLIT_RECORDER`` do not support choosing a codec: they only accept ``DEFAULT``.
* ``DEFAULT`` is valid with every profile.

If an invalid combination is requested, the RecorderEndpoint is **not created**: the call fails with a ``MEDIA_OBJECT_ILLEGAL_PARAM_ERROR`` error, whose message indicates the offending codec and profile. For example, requesting ``H265`` together with ``WEBM``.

.. note::

   Only H.264 and H.265 in MP4 and MKV are covered by automated tests in the current release. The remaining combinations of the table are accepted by the API, but they depend on the codec support of the GStreamer plugins that are installed in the media server.



Passthrough and transcoding
===========================

Kurento compares the codec of the incoming media with the codec requested for the recording:

* **They match: passthrough.** The video is stored as-is, without being decoded nor encoded again. There is no encoder in the media pipeline, so the CPU usage is minimal and the quality is not degraded.
* **They differ: transcoding.** The video is decoded and encoded again with the requested codec. This incurs in a noticeable CPU load, and a ``MediaTranscodingStateChanged`` event is emitted by the source element, which you can use to detect that this is happening.

Some practical notes:

* **PlayerEndpoint** decodes the media by default. To get passthrough when recording a file or stream, create the player with *useEncodedMedia* enabled.
* **RtpEndpoint** and **WebRtcEndpoint** deliver the codec that was negotiated in the SDP. To record H.265 without transcoding, the remote peer must send H.265, and the endpoint must have negotiated it.
* H.265 video is accepted at the recorder in any of its bitstream formats (``hvc1``, ``hev1`` or ``byte-stream``) and it is always written to the file as ``hvc1``, so it is not necessary to take care of that in the sender.
* If you ask for ``H265`` but the source sends another codec, Kurento transcodes to H.265. This requires the *x265* encoder (part of the GStreamer *bad* plugins set) to be available in the media server.



Examples
========

Record H.265 into an MP4 file:

.. code-block:: java

   // Java
   RecorderEndpoint recorder = new RecorderEndpoint
       .Builder(pipeline, "file:///var/lib/kurento/recording.mp4")
       .withMediaProfile(MediaProfileSpecType.MP4)
       .withVideoCodec(RecorderVideoCodec.H265)
       .build();

.. code-block:: js

   // JavaScript
   const recorder = await pipeline.create("RecorderEndpoint", {
     uri: "file:///var/lib/kurento/recording.mp4",
     mediaProfile: "MP4",
     videoCodec: "H265",
   });

Record from a file with H.265 video, without transcoding:

.. code-block:: java

   // Java
   PlayerEndpoint player = new PlayerEndpoint
       .Builder(pipeline, "file:///var/lib/kurento/source-h265.mp4")
       .useEncodedMedia()
       .build();

   RecorderEndpoint recorder = new RecorderEndpoint
       .Builder(pipeline, "file:///var/lib/kurento/copy.mkv")
       .withMediaProfile(MediaProfileSpecType.MKV_VIDEO_ONLY)
       .withVideoCodec(RecorderVideoCodec.H265)
       .build();

   player.connect(recorder, MediaType.VIDEO);
   recorder.record();
   player.play();

.. code-block:: js

   // JavaScript
   const player = await pipeline.create("PlayerEndpoint", {
     uri: "file:///var/lib/kurento/source-h265.mp4",
     useEncodedMedia: true,
   });

   const recorder = await pipeline.create("RecorderEndpoint", {
     uri: "file:///var/lib/kurento/copy.mkv",
     mediaProfile: "MKV_VIDEO_ONLY",
     videoCodec: "H265",
   });

   await player.connect(recorder, "VIDEO");
   await recorder.record();
   await player.play();



Verifying the result
====================

Use ``ffprobe`` (from the FFmpeg project) to check the codec that was actually written to the file:

.. code-block:: shell

   ffprobe -v error -select_streams v:0 \
       -show_entries stream=codec_name,codec_tag_string \
       -of default=noprint_wrappers=1 recording.mp4

For an H.265 MP4 file, the expected output is:

.. code-block:: text

   codec_name=hevc
   codec_tag_string=hvc1

In MKV files, the codec is described by the container itself (a *hvcC* record), so the tag is not shown; checking that ``codec_name`` is ``hevc`` is enough.



Playback of H.265 files: hvc1 and Safari
========================================

H.265 can be stored in MP4 with two different tags: ``hvc1`` and ``hev1``. They differ in where the parameter sets (VPS, SPS, PPS) are located. Apple's players (Safari, QuickTime) only play ``hvc1`` files, while ``hev1`` files are rejected. For this reason, Kurento always writes ``hvc1``.

H.265 playback in web browsers depends on the browser and on the hardware decoder of the device; check the support of your target platforms before relying on it. See also :doc:`/knowledge/safari`.



H.265 licensing
===============

H.265/HEVC is covered by patents managed by several licensing pools, and the *x265* encoder used by Kurento for transcoding is distributed under the GNU GPL v2 license (with a commercial license available from its authors). Using H.265 may require paying royalties or having a license, depending on your use case and jurisdiction. Kurento does not grant any patent license: it is **your responsibility** to verify that your usage is legal. Recording H.265 by passthrough does not involve any encoder, but it still produces H.265 content.



Minimum server version
======================

The *videoCodec* parameter is available since Kurento Media Server **7.4.0** (see the :doc:`/project/relnotes/7.4`), and in the matching versions of the Java and JavaScript clients.

Using a new client against an **older server** does not produce an error: servers ignore constructor parameters that they do not know about. The RecorderEndpoint will then be created with the default codec of the container (e.g. H.264 for MP4), so *videoCodec* has no effect. If your application requires a given codec, check the version of the server it connects to, or inspect the recorded files with ``ffprobe``.

An **old client** against a new server keeps working as before, because *videoCodec* is optional.
