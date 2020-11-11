======================
RTP Streaming Commands
======================

.. contents:: Table of Contents

In this document you will find several examples of command-line programs that can be used to generate RTP and SRTP streams. These streams can then be used to feed any general (S)RTP receiver, although the intention here is to use them to connect an *RtpEndpoint* from a Kurento Media Server pipeline.

The tool used for all these programs is `gst-launch <https://gstreamer.freedesktop.org/documentation/tools/gst-launch.html>`__, part of the GStreamer multimedia library.

These examples start from the simplest and then build on each other to end up with a full featured RTP generator. Of course, as more features are added, the command grows in complexity. A very good understanding of *gst-launch* and of GStreamer is recommended.

To run these examples, follow these initial steps:

1. Install required packages:

   .. code-block:: console

      sudo apt-get update && sudo apt-get install --no-install-recommends --yes \
          gstreamer1.0-{tools,libav} \
          gstreamer1.0-plugins-{base,good,bad,ugly}

2. [Optional] Enable debug logging:

   .. code-block:: console

      export GST_DEBUG=3

3. Copy & paste these commands into a console.

4. Write a Kurento application that does this:

   .. code-block:: text

      sdpAnswer = rtpEndpoint.processOffer(sdpOffer);
      log.info("SDP Answer: {}", sdpAnswer);

5. Start pipeline (e.g. in the :doc:`RTP Receiver tutorial </tutorials/java/tutorial-rtp-receiver>`, push "Start")
6. From the logs: get the KMS port from the SDP Answer (in the RTP Receiver tutorial, this appears in the web page)
7. Set *PEER_V* in the *gst-launch* commands to the KMS port.



RTP sender examples
===================

Simplest RTP sender
-------------------

Features:

- Video RTP sender

.. code-block:: console

    PEER_V=9004 PEER_IP=127.0.0.1 \
    SELF_PATH="$PWD/video.mp4" \
    bash -c 'gst-launch-1.0 -e \
        uridecodebin uri="file://$SELF_PATH" \
            ! videoconvert ! x264enc tune=zerolatency \
            ! rtph264pay \
            ! "application/x-rtp,payload=(int)103,clock-rate=(int)90000" \
            ! udpsink host=$PEER_IP port=$PEER_V'



Example 2
---------

Features:

- Video RTP sender
- Video RTCP receiver

.. code-block:: console

    PEER_V=9004 PEER_IP=127.0.0.1 \
    SELF_PATH="$PWD/video.mp4" \
    SELF_V=5004 SELF_VSSRC=112233 \
    bash -c 'gst-launch-1.0 -e \
        rtpsession name=r sdes="application/x-rtp-source-sdes,cname=(string)\"user\@example.com\"" \
        uridecodebin uri="file://$SELF_PATH" \
            ! videoconvert ! x264enc tune=zerolatency \
            ! rtph264pay \
            ! "application/x-rtp,payload=(int)103,clock-rate=(int)90000,ssrc=(uint)$SELF_VSSRC" \
            ! r.send_rtp_sink \
        r.send_rtp_src \
            ! udpsink host=$PEER_IP port=$PEER_V \
        udpsrc port=$((SELF_V+1)) \
            ! r.recv_rtcp_sink'



Example 3
---------

Features:

- Video RTP sender
- Video RTCP receiver console dump

.. code-block:: console

    PEER_V=9004 PEER_IP=127.0.0.1 \
    SELF_PATH="$PWD/video.mp4" \
    SELF_V=5004 SELF_VSSRC=112233 \
    bash -c 'gst-launch-1.0 -e \
        rtpsession name=r sdes="application/x-rtp-source-sdes,cname=(string)\"user\@example.com\"" \
        uridecodebin uri="file://$SELF_PATH" \
            ! videoconvert ! x264enc tune=zerolatency \
            ! rtph264pay \
            ! "application/x-rtp,payload=(int)103,clock-rate=(int)90000,ssrc=(uint)$SELF_VSSRC" \
            ! r.send_rtp_sink \
        r.send_rtp_src \
            ! udpsink host=$PEER_IP port=$PEER_V \
        udpsrc port=$((SELF_V+1)) \
            ! tee name=t \
            t. ! queue ! r.recv_rtcp_sink \
            t. ! queue ! fakesink dump=true async=false'



Example 4
---------

Features:

- Video RTP & RTCP sender
- Video RTCP receiver console dump

.. code-block:: console

    PEER_V=9004 PEER_IP=127.0.0.1 \
    SELF_PATH="$PWD/video.mp4" \
    SELF_V=5004 SELF_VSSRC=112233 \
    bash -c 'gst-launch-1.0 -e \
        rtpsession name=r sdes="application/x-rtp-source-sdes,cname=(string)\"user\@example.com\"" \
        uridecodebin uri="file://$SELF_PATH" \
            ! videoconvert ! x264enc tune=zerolatency \
            ! rtph264pay \
            ! "application/x-rtp,payload=(int)103,clock-rate=(int)90000,ssrc=(uint)$SELF_VSSRC" \
            ! r.send_rtp_sink \
        r.send_rtp_src \
            ! udpsink host=$PEER_IP port=$PEER_V \
        r.send_rtcp_src \
            ! udpsink host=$PEER_IP port=$((PEER_V+1)) sync=false async=false \
        udpsrc port=$((SELF_V+1)) \
            ! tee name=t \
            t. ! queue ! r.recv_rtcp_sink \
            t. ! queue ! fakesink dump=true async=false'



Example 5
---------

Features:

- Video RTP & RTCP sender
- Video RTCP receiver console dump
- Symmetrical ports (for autodiscovery)

.. code-block:: console

    PEER_V=9004 PEER_IP=127.0.0.1 \
    SELF_PATH="$PWD/video.mp4" \
    SELF_V=5004 SELF_VSSRC=112233 \
    bash -c 'gst-launch-1.0 -e \
        rtpsession name=r sdes="application/x-rtp-source-sdes,cname=(string)\"user\@example.com\"" \
        uridecodebin uri="file://$SELF_PATH" \
            ! videoconvert ! x264enc tune=zerolatency \
            ! rtph264pay \
            ! "application/x-rtp,payload=(int)103,clock-rate=(int)90000,ssrc=(uint)$SELF_VSSRC" \
            ! r.send_rtp_sink \
        r.send_rtp_src \
            ! udpsink host=$PEER_IP port=$PEER_V bind-port=$SELF_V \
        r.send_rtcp_src \
            ! udpsink host=$PEER_IP port=$((PEER_V+1)) bind-port=$((SELF_V+1)) sync=false async=false \
        udpsrc port=$((SELF_V+1)) \
            ! tee name=t \
            t. ! queue ! r.recv_rtcp_sink \
            t. ! queue ! fakesink dump=true async=false'



Example 6
---------

Features:

- Audio RTP & RTCP sender
- Video RTCP receiver console dump
- Symmetrical ports (for autodiscovery)

.. code-block:: console

    PEER_A=9006 PEER_IP=127.0.0.1 \
    SELF_A=5006 SELF_ASSRC=445566 \
    bash -c 'gst-launch-1.0 -e \
        rtpsession name=r sdes="application/x-rtp-source-sdes,cname=(string)\"user\@example.com\"" \
        audiotestsrc volume=0.5 \
            ! audioconvert ! audioresample ! opusenc \
            ! rtpopuspay \
            ! "application/x-rtp,payload=(int)96,clock-rate=(int)48000,ssrc=(uint)$SELF_ASSRC" \
            ! r.send_rtp_sink \
        r.send_rtp_src \
            ! udpsink host=$PEER_IP port=$PEER_A bind-port=$SELF_A \
        r.send_rtcp_src \
            ! udpsink host=$PEER_IP port=$((PEER_A+1)) bind-port=$((SELF_A+1)) sync=false async=false \
        udpsrc port=$((SELF_A+1)) \
            ! tee name=t \
            t. ! queue ! r.recv_rtcp_sink \
            t. ! queue ! fakesink dump=true async=false'



Full-featured RTP sender
------------------------

Features:

- Audio & Video RTP & RTCP sender
- Audio & Video RTCP receiver
- Video RTCP receiver console dump
- Symmetrical ports (for autodiscovery)

.. code-block:: console

    PEER_A=9006 PEER_V=9004 PEER_IP=127.0.0.1 \
    SELF_PATH="$PWD/video.mp4" \
    SELF_A=5006 SELF_ASSRC=445566 \
    SELF_V=5004 SELF_VSSRC=112233 \
    bash -c 'gst-launch-1.0 -e \
        rtpbin name=r sdes="application/x-rtp-source-sdes,cname=(string)\"user\@example.com\"" \
        uridecodebin uri="file://$SELF_PATH" name=d \
        d. ! queue \
            ! audioconvert ! audioresample ! opusenc \
            ! rtpopuspay \
            ! "application/x-rtp,payload=(int)96,clock-rate=(int)48000,ssrc=(uint)$SELF_ASSRC" \
            ! r.send_rtp_sink_0 \
        d. ! queue \
            ! videoconvert ! x264enc tune=zerolatency \
            ! rtph264pay \
            ! "application/x-rtp,payload=(int)103,clock-rate=(int)90000,ssrc=(uint)$SELF_VSSRC" \
            ! r.send_rtp_sink_1 \
        r.send_rtp_src_0 \
            ! udpsink host=$PEER_IP port=$PEER_A bind-port=$SELF_A \
        r.send_rtcp_src_0 \
            ! udpsink host=$PEER_IP port=$((PEER_A+1)) bind-port=$((SELF_A+1)) sync=false async=false \
        udpsrc port=$((SELF_A+1)) \
            ! r.recv_rtcp_sink_0 \
        r.send_rtp_src_1 \
            ! udpsink host=$PEER_IP port=$PEER_V bind-port=$SELF_V \
        r.send_rtcp_src_1 \
            ! udpsink host=$PEER_IP port=$((PEER_V+1)) bind-port=$((SELF_V+1)) sync=false async=false \
        udpsrc port=$((SELF_V+1)) \
            ! tee name=t \
            t. ! queue ! r.recv_rtcp_sink_1 \
            t. ! queue ! fakesink dump=true async=false'



RTP receiver examples
=====================

Example 1
---------

Features:

- Video RTP & RTCP receiver
- RTCP sender

.. code-block:: console

    PEER_V=5004 PEER_IP=127.0.0.1 \
    SELF_V=9004 \
    CAPS_V="media=(string)video,clock-rate=(int)90000,encoding-name=(string)H264,payload=(int)103" \
    bash -c 'gst-launch-1.0 -e \
        rtpsession name=r sdes="application/x-rtp-source-sdes,cname=(string)\"user\@example.com\"" \
        udpsrc port=$SELF_V \
            ! "application/x-rtp,$CAPS_V" \
            ! r.recv_rtp_sink \
        r.recv_rtp_src \
            ! rtph264depay \
            ! decodebin \
            ! autovideosink \
        udpsrc port=$((SELF_V+1)) \
            ! r.recv_rtcp_sink \
        r.send_rtcp_src \
            ! udpsink host=$PEER_IP port=$((PEER_V+1)) sync=false async=false'

.. note::

   RtpSession is used to handle RTCP, and it needs explicit video caps.



Example 2
---------

Features:

- Audio & Video RTP & RTCP receiver
- Video RTCP receiver console dump
- Audio & Video RTCP sender
- Symmetrical ports (for autodiscovery)

.. code-block:: console

    PEER_A=5006 PEER_ASSRC=445566 PEER_V=5004 PEER_VSSRC=112233 PEER_IP=127.0.0.1 \
    SELF_A=9006 SELF_V=9004 \
    CAPS_A="media=(string)audio,clock-rate=(int)48000,encoding-name=(string)OPUS,payload=(int)96" \
    CAPS_V="media=(string)video,clock-rate=(int)90000,encoding-name=(string)H264,payload=(int)103" \
    bash -c 'gst-launch-1.0 -e \
        rtpbin name=r sdes="application/x-rtp-source-sdes,cname=(string)\"user\@example.com\"" \
        udpsrc port=$SELF_A \
            ! "application/x-rtp,$CAPS_A" \
            ! r.recv_rtp_sink_0 \
        r.recv_rtp_src_0_${PEER_ASSRC}_96 \
            ! rtpopusdepay \
            ! decodebin \
            ! autoaudiosink \
        udpsrc port=$((SELF_A+1)) \
            ! r.recv_rtcp_sink_0 \
        r.send_rtcp_src_0 \
            ! udpsink host=$PEER_IP port=$((PEER_A+1)) bind-port=$((SELF_A+1)) sync=false async=false \
        udpsrc port=$SELF_V \
            ! "application/x-rtp,$CAPS_V" \
            ! r.recv_rtp_sink_1 \
        r.recv_rtp_src_1_${PEER_VSSRC}_103 \
            ! rtph264depay \
            ! decodebin \
            ! autovideosink \
        udpsrc port=$((SELF_V+1)) \
            ! tee name=t \
            t. ! queue ! r.recv_rtcp_sink_1 \
            t. ! queue ! fakesink dump=true async=false \
        r.send_rtcp_src_1 \
            ! udpsink host=$PEER_IP port=$((PEER_V+1)) bind-port=$((SELF_V+1)) sync=false async=false'



SRTP examples
=============

For the SRTP examples, you need to install the Kurento's fork of GStreamer:

.. code-block:: console

   sudo apt-get update && sudo apt-get install --no-install-recommends --yes \
       gstreamer1.5-{tools,libav} \
       gstreamer1.5-plugins-{base,good,bad,ugly}



SRTP simple sender
------------------

Features:

- Video SRTP sender

.. code-block:: console

    PEER_V=9004 PEER_IP=127.0.0.1 \
    SELF_PATH="$PWD/video.mp4" \
    SELF_VSSRC=112233 \
    SELF_KEY="4142434445464748494A4B4C4D4E4F505152535455565758595A31323334" \
    bash -c 'gst-launch-1.5 -e \
        uridecodebin uri="file://$SELF_PATH" \
        ! videoconvert \
        ! x264enc tune=zerolatency \
        ! rtph264pay \
        ! "application/x-rtp,payload=(int)103,ssrc=(uint)$SELF_VSSRC" \
        ! srtpenc key="$SELF_KEY" \
            rtp-cipher="aes-128-icm" rtp-auth="hmac-sha1-80" \
            rtcp-cipher="aes-128-icm" rtcp-auth="hmac-sha1-80" \
        ! udpsink host=$PEER_IP port=$PEER_V'



SRTP simple receiver
--------------------

Features:

- Video SRTP receiver

.. code-block:: console

    PEER_VSSRC=112233 \
    PEER_KEY="4142434445464748494A4B4C4D4E4F505152535455565758595A31323334" \
    SELF_V=9004 \
    SRTP_CAPS="payload=(int)103,ssrc=(uint)$PEER_VSSRC,roc=(uint)0, \
        srtp-key=(buffer)$PEER_KEY, \
        srtp-cipher=(string)aes-128-icm,srtp-auth=(string)hmac-sha1-80, \
        srtcp-cipher=(string)aes-128-icm,srtcp-auth=(string)hmac-sha1-80" \
    bash -c 'gst-launch-1.5 -e \
        udpsrc port=$SELF_V \
        ! "application/x-srtp,$SRTP_CAPS" \
        ! srtpdec \
        ! rtph264depay \
        ! decodebin \
        ! autovideosink'

.. note::

   No RtpSession is used to handle RTCP, so no need for explicit video caps.



SRTP complete sender
--------------------

Features:

- Video SRTP & SRTCP sender
- SRTCP receiver console dump

.. code-block:: console

    PEER_V=9004 PEER_VSSRC=332211 PEER_IP=127.0.0.1 \
    PEER_KEY="343332315A595857565554535251504F4E4D4C4B4A494847464544434241" \
    SELF_PATH="$PWD/video.mp4" \
    SELF_V=5004 SELF_VSSRC=112233 \
    SELF_KEY="4142434445464748494A4B4C4D4E4F505152535455565758595A31323334" \
    SRTP_CAPS="payload=(int)103,ssrc=(uint)$PEER_VSSRC,roc=(uint)0, \
        srtp-key=(buffer)$PEER_KEY, \
        srtp-cipher=(string)aes-128-icm,srtp-auth=(string)hmac-sha1-80, \
        srtcp-cipher=(string)aes-128-icm,srtcp-auth=(string)hmac-sha1-80" \
    bash -c 'gst-launch-1.5 -e \
        rtpsession name=r sdes="application/x-rtp-source-sdes,cname=(string)\"user\@example.com\"" \
        srtpenc name=e key="$SELF_KEY" \
            rtp-cipher="aes-128-icm" rtp-auth="hmac-sha1-80" \
            rtcp-cipher="aes-128-icm" rtcp-auth="hmac-sha1-80" \
        srtpdec name=d \
        uridecodebin uri="file://$SELF_PATH" \
            ! videoconvert ! x264enc tune=zerolatency \
            ! rtph264pay \
            ! "application/x-rtp,payload=(int)103,ssrc=(uint)$SELF_VSSRC" \
            ! r.send_rtp_sink \
        r.send_rtp_src \
            ! e.rtp_sink_0 \
        e.rtp_src_0 \
            ! udpsink host=$PEER_IP port=$PEER_V \
        r.send_rtcp_src \
            ! e.rtcp_sink_0 \
        e.rtcp_src_0 \
            ! udpsink host=$PEER_IP port=$((PEER_V+1)) sync=false async=false \
        udpsrc port=$((SELF_V+1)) \
            ! "application/x-srtcp,$SRTP_CAPS" \
            ! d.rtcp_sink \
        d.rtcp_src \
            ! tee name=t \
            t. ! queue ! r.recv_rtcp_sink \
            t. ! queue ! fakesink dump=true async=false'



SRTP complete receiver
----------------------

Features:

- Video SRTP & SRTCP receiver
- SRTCP sender

.. code-block:: console

    PEER_V=5004 PEER_VSSRC=112233 PEER_IP=127.0.0.1 \
    PEER_KEY="4142434445464748494A4B4C4D4E4F505152535455565758595A31323334" \
    SELF_V=9004 SELF_VSSRC=332211 \
    SELF_KEY="343332315A595857565554535251504F4E4D4C4B4A494847464544434241" \
    SRTP_CAPS="payload=(int)103,ssrc=(uint)$PEER_VSSRC,roc=(uint)0, \
        srtp-key=(buffer)$PEER_KEY, \
        srtp-cipher=(string)aes-128-icm,srtp-auth=(string)hmac-sha1-80, \
        srtcp-cipher=(string)aes-128-icm,srtcp-auth=(string)hmac-sha1-80" \
    CAPS_V="media=(string)video,clock-rate=(int)90000,encoding-name=(string)H264,payload=(int)103" \
    bash -c 'gst-launch-1.5 -e \
        rtpsession name=r sdes="application/x-rtp-source-sdes,cname=(string)\"recv\@example.com\"" \
        srtpenc name=e key="$SELF_KEY" \
            rtp-cipher="aes-128-icm" rtp-auth="hmac-sha1-80" \
            rtcp-cipher="aes-128-icm" rtcp-auth="hmac-sha1-80" \
        srtpdec name=d \
        udpsrc port=$SELF_V \
            ! "application/x-srtp,$SRTP_CAPS" \
            ! d.rtp_sink \
        d.rtp_src \
            ! "application/x-rtp,$CAPS_V" \
            ! r.recv_rtp_sink \
        r.recv_rtp_src \
            ! rtph264depay \
            ! decodebin \
            ! autovideosink \
        udpsrc port=$((SELF_V+1)) \
            ! "application/x-srtcp,$SRTP_CAPS" \
            ! d.rtcp_sink \
        d.rtcp_src \
            ! r.recv_rtcp_sink \
        fakesrc num-buffers=-1 sizetype=2 \
            ! "application/x-rtp,payload=(int)103,ssrc=(uint)$SELF_VSSRC" \
            ! r.send_rtp_sink \
        r.send_rtp_src \
            ! fakesink async=false \
        r.send_rtcp_src \
            ! e.rtcp_sink_0 \
        e.rtcp_src_0 \
            ! udpsink host=$PEER_IP port=$((PEER_V+1)) sync=false async=false'

.. note::

   *fakesrc* is used to force *rtpsession* to use the desired SSRC.



Additional Notes
================

These are some random and unstructured notes that don't have the same level of detail as the previous section. They are here just as a way of taking note of alternative methods or useful bits of information, but don't expect that any command from this section works at all.



About 'sync=false'
------------------

Pipeline initialization is done with 3 state changes:

1. NULL -> READY: Underlying devices are probed to ensure they can be accessed.
2. READY -> PAUSED: Preroll is done, which means that an initial frame is brought from the sources and set into the sinks of the pipeline.
3. PAUSED -> PLAYING: Sources start generating frames, and sinks start receiving and processing them.

The **sync** property indicates whether the element is Live (``sync=true``) or Non-Live (``sync=false``):

- Live elements are synchronized against the clock, and only process data according to the established rate. The timestamps of the incoming buffers will be used to schedule the exact render time of its contents.
- Non-Live elements do not synchronize with any clock, and process data as fast as possible. The pipeline will ignore the timestamps of the video frames and it will play them as fast as they arrive, ignoring all timing information. Note that setting "sync=false" is almost never a solution when timing-related problems occur.

For example, a video camera or an output window/screen would be Live elements; a local file would be a Non-Live element.

The **async** property enables (``async=true``) or disables (``async=false``) the Preroll feature:

- Live sources cannot produce an initial frame until they are set to PLAYING state, so Preroll cannot be done with them on PAUSE state. If Prerolling is enabled in a Live sink, it will be set on hold waiting for that initial frame to arrive, and only then they will be able to complete the Preroll and start playing.
- Non-Live sources should be able to produce an initial frame before reaching the PLAYING state, allowing their downstream sinks to Preroll as soon as the PAUSED state is set.

Since RTCP packets from the sender should be sent as soon as possible and do not participate in preroll, ``sync=false`` and ``async=false`` are configured on *udpsink*.

See:

* https://gstreamer.freedesktop.org/data/doc/gstreamer/head/gst-plugins-good-plugins/html/gst-plugins-good-plugins-rtpbin.html
* https://gstreamer.freedesktop.org/documentation/design/latency.html



About the SRTP Master Key
-------------------------

The SRTP Master Key is the concatenation of (key, salt). With *AES_CM_128* + *HMAC_SHA1_80*, Master Key is 30 bytes: 16 bytes key + 14 bytes salt.

Key formats:

- GStreamer (*gst-launch*): Hexadecimal.
- Kurento (*RtpEndpoint*): ASCII.
- SDP Offer/Answer: Base64.

Use this website to convert between formats: https://tomeko.net/online_tools/hex_to_base64.php

Encryption key used by the **sender** examples:

- ASCII: ``ABCDEFGHIJKLMNOPQRSTUVWXYZ1234``.
- In Hex: ``4142434445464748494A4B4C4D4E4F505152535455565758595A31323334``.
- In Base64: ``QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVoxMjM0``.

Encryption key used by the **receiver** examples:

- ASCII: ``4321ZYXWVUTSRQPONMLKJIHGFEDCBA``.
- In Hex: ``343332315A595857565554535251504F4E4D4C4B4A494847464544434241``.
- In Base64: ``NDMyMVpZWFdWVVRTUlFQT05NTEtKSUhHRkVEQ0JB``.



Using FFmpeg
------------

It should be possible to use FFmpeg to send or receive RTP streams; just make sure that all stream details match between the SDP negotiation and the actual encoded stream. For example: reception ports, Payload Type, encoding settings, etc.

This command is a good starting point to send RTP:

.. code-block:: console

   ffmpeg -re -i "video.mp4" -c:v libx264 -tune zerolatency -payload_type 103 \
       -an -f rtp rtp://IP:PORT

Note that Payload Type is **103** in these and all other examples, because that's the number used in the SDP Offer sent to the *RtpEndpoint* in Kurento. You could use any other number, just make sure that it gets used consistently in both SDP Offer and RTP sender program.



SDP Offer examples
------------------

Some examples of the SDP Offer that should be sent to Kurento's *RtpEndpoint* to configure it with needed parameters for the RTP sender examples shown in this page:


**Audio & Video RTP & RTCP sender**

A basic SDP message that describes a simple Audio + Video RTP stream.

.. code-block:: text

    v=0
    o=- 0 0 IN IP4 127.0.0.1
    s=-
    c=IN IP4 127.0.0.1
    t=0 0
    m=audio 5006 RTP/AVP 96
    a=rtpmap:96 opus/48000/2
    a=sendonly
    a=ssrc:445566 cname:user@example.com
    m=video 5004 RTP/AVP 103
    a=rtpmap:103 H264/90000
    a=sendonly
    a=ssrc:112233 cname:user@example.com


Some modifications that would be done for KMS:

- Add support for :doc:`REMB Congestion Control </knowledge/congestion_rmcat>`.
- Add symmetrical ports (for :ref:`Port Autodiscovery <features-comedia>`).

.. code-block:: text

    v=0
    o=- 0 0 IN IP4 127.0.0.1
    s=-
    c=IN IP4 127.0.0.1
    t=0 0
    m=audio 5006 RTP/AVP 96
    a=rtpmap:96 opus/48000/2
    a=sendonly
    a=direction:active
    a=ssrc:445566 cname:user@example.com
    m=video 5004 RTP/AVPF 103
    a=rtpmap:103 H264/90000
    a=rtcp-fb:103 goog-remb
    a=sendonly
    a=direction:active
    a=ssrc:112233 cname:user@example.com
