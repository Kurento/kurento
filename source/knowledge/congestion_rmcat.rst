==========================
Congestion Control (RMCAT)
==========================

*RTP Media Congestion Avoidance Techniques* (RMCAT) is an `IETF Working Group`__ that aims to develop new protocols which can manage network congestion in the context of RTP streaming. The goals for any congestion control algorithm are:

- Preventing network collapse due to congestion.
- Allowing multiple flows to share the network fairly.

A good introduction to RMCAT, its history and its context can be found in this blog post by Mozilla: `What is RMCAT congestion control, and how will it affect WebRTC?`__.

As a result of this Working Group, several algorithms have been proposed so far by different vendors:

-  By Cisco: `NADA`__, *A Unified Congestion Control Scheme for Real-Time Media*.
-  By Ericsson: `SCReAM`__, *Self-Clocked Rate Adaptation for Multimedia*.
-  By Google: `GCC`__, *Google Congestion Control Algorithm for Real-Time Communication*.

.. __: https://tools.ietf.org/html/rfc7295
.. __: https://blog.mozilla.org/webrtc/what-is-rmcat-congestion-control/
.. __: https://tools.ietf.org/html/draft-ietf-rmcat-nada
.. __: https://tools.ietf.org/html/rfc8298
.. __: https://tools.ietf.org/html/draft-ietf-rmcat-gcc



Google Congestion Control
=========================

Google's GCC is the RMCAT algorithm of choice for WebRTC, and it's used by WebRTC-compatible web browsers. In GCC, both sender and receiver of an RTP session collaborate to find out a realistic estimation of the actual network bandwidth that is available:

- The RTP sender generates special timestamps called *abs-send-time*, and sends them as part of the RTP packet's Header Extension.
- The RTP receiver generates a new type of RTCP Feedback message called *Receiver Estimated Maximum Bitrate* (REMB). These messages are appended to normal RTCP packets.



Meaning of REMB
===============

There has been some misconceptions about what is the exact meaning of the value that is carried by REMB messages. In short, REMB is used for reporting the aggregate bandwidth estimates of the receiver, across all media streams that are sharing the same RTP session.

Sources:

- In `[rmcat] comments on draft-alvestrand-rmcat-remb-02`__, this question is responded:

  .. code-block:: text

     > - the maximum bitrate, is it defined as the maximum bitrate for
     >   a particular stream, or the maximum bitrate for the whole "Session" ?

     As per GCC, REMB can be used for reporting the sum of the receiver
     estimate across all media streams that share the same end-to-end path.
     I supposed the SSRCs of the multiple media streams that make up the
     aggregate estimate are reported in the block.

- In `[rtcweb] REMB with many ssrc entries`__, a similar question is answered, talking about an explicit example:

  .. code-block:: text

     > If Alice is sending Bob an audio stream (SSRC 1111) and a video stream
     > (SSRC 2222) and Bob sends a REMB feedback message with:
     >   - bitrate: 100000 (100kbits/s)
     >   - ssrcs: 1111, 2222
     >
     > Does it mean that Alice should limit the sum of her sending audio and
     > video bitrates to 100kbits/s? or does it mean that Alice can send
     > 100kbits/s of audio and 100kbits/s of video (total = 200)?

     The way it was originally designed, it meant that the total should be
     100 Kbits/sec. REMB did not take a position on how the sender chose to
     allocate bits between the SSRCs, only the total amount of bits sent.

.. __: https://mailarchive.ietf.org/arch/msg/rmcat/5Y32E-UwdxckFn1gIMIwaKEiorw
.. __: https://mailarchive.ietf.org/arch/msg/rtcweb/5gFDsUTzS2zQM8Znic1IYUQ3jQI
