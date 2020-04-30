=============
NAT Traversal
=============

:term:`NAT Traversal`, also known as *Hole Punching*, is the procedure of opening an inbound port in the :term:`NAT` tables of the routers which implement this technology (which are the vast majority of home and corporate routers).

There are different types of NAT, depending on how they behave: **Full Cone**, **Address-Restricted Cone**, **Port-Restricted Cone**, and **Symmetric**. For a comprehensive explanation of NAT and the different types that exist, please read our Knowledge Base document: :doc:`/knowledge/nat`.



WebRTC with ICE
===============

:term:`ICE` is the standard method used by :term:`WebRTC` to solve the issue of :term:`NAT Traversal`. Kurento supports ICE by means of a 3rd-party library: `libnice, The GLib ICE implementation <https://nice.freedesktop.org>`__.

Refer to the :ref:`logging documentation <logging-libnice>` if you need to enable the debug logging for this library.



.. _features-comedia:

RTP without ICE
===============

KMS is able to automatically infer what is the public IP and port of any remote peer which is communicating with it through an RTP connection. This removes the need to use ICE in some specific situations, where that complicated mechanism is not desired. This new automatic port discovery was inspired by the **Connection-Oriented Media Transport** (COMEDIA) as presented by the early Drafts of what finally would become the RFC 4145.

**TCP-Based Media Transport in the Session Description Protocol** (SDP) (`IETF RFC 4145 <https://tools.ietf.org/html/rfc4145>`__) defines an SDP extension which adds TCP connections and procedures, such as how a passive machine would wait for connections from a remote active machine and be able to obtain connection information from the active one, upon reception of an initial connection.

Early Drafts of RFC 4145 (up to `Draft 05 <https://tools.ietf.org/html/draft-ietf-mmusic-sdp-comedia-05>`__) also contemplated the usage of this same concept of "Connection-Oriented Media Transport in SDP" with UDP connections, as a way of aiding :term:`NAT Traversal`. This is what has been used as a basis for the implementation of automatic port discovery in KMS.

It works as follows:

1. The machine behind a :term:`NAT` router acts as the active peer. It sends an SDP Offer to the other machine, the passive peer.

   A. Sending an SDP Offer from behind a NAT means that the IP and port specified in the SDP message are actually just the private IP and port of that machine, instead of the public ones. The passive peer won't be able to use these to communicate back to the active peer. Due to this, the SDP Offer states the port ``9`` (*Discard port*) instead of whatever port the active machine will be using.
   B. The SDP Offer includes the media-level attribute ``a=direction:active``, so the passive peer is able to acknowledge that the Connection-Oriented Media Transport is being used for that media, and it writes ``a=direction:passive`` in its SDP Answer.

2. The passive peer receives the SDP Offer and answers it as usual, indicating the public IP and port where it will be listening for incoming packets. Besides that, it must ignore the IP and port indicated in the received SDP Offer. Instead, it must enter a wait state, until the active peer starts sending some packets.

3. When the active peer sends the first RTP/RTCP packets to the IP and port specified in the SDP Answer, the passive peer will be able to analyze them on reception and extract the public IP and reception port of the active peer.

4. The passive peer is now able to send RTP/RTCP packets to the discovered IP and port values of the active peer.

This mechanism has the following requisites and/or limitations:

- Only the active peer can be behind a NAT router. The passive peer must have a publicly accessible IP and port for RTP.
- The active peer must be able to receive RTP/RTCP packets at the same ports that are used to send RTP/RTCP packets. In other words, the active peer must be compatible with *Symmetric RTP and RTCP* as defined in `IETF RFC 4961 <https://tools.ietf.org/html/rfc4961>`__.
- The active peer must actually do send some RTP/RTCP packets before the passive peer is able to send any data back. In other words, it is not possible to establish a one-way stream where only the passive peer sends data to the active peer.

This is how to enable the Connection-Oriented Media Transport mode:

- The SDP Offer must be sent from the active peer to the passive peer.
- The IP stated in the SDP Offer can be anything (as it will be ignored), so ``0.0.0.0`` can be used.
- The Port stated in the SDP Offer should be ``9`` (*Discard port*).
- The active peer must include the media-level attribute ``a=direction:active`` in the SDP Offer, for each media that requires automatic port discovery.
- The passive peer must acknowledge that it supports the automatic port discovery mode, by including the media-level attribute ``a=direction:passive`` in its SDP Answer. As per normal rules of the SDP Offer/Answer Model (`IETF RFC 3264 <https://tools.ietf.org/html/rfc3264>`__), if this attribute is not present in the SDP Answer, then the active peer must assume that the passive peer is not compatible with this functionality and should react to this fact as whatever is deemed appropriate by the application developer.



Example
-------

This is a minimal example of an :term:`SDP Offer/Answer` negotiation that a machine would perform with KMS from behind a :term:`NAT` router. The highlighted lines are those relevant to NAT Traversal:

.. code-block:: text
   :caption: SDP Offer
   :emphasize-lines: 6,9,11,14

   v=0
   o=- 0 0 IN IP4 0.0.0.0
   s=Example sender
   c=IN IP4 0.0.0.0
   t=0 0
   m=audio 9 RTP/AVPF 96
   a=rtpmap:96 opus/48000/2
   a=sendonly
   a=direction:active
   a=ssrc:111111 cname:active@example.com
   m=video 9 RTP/AVPF 103
   a=rtpmap:103 H264/90000
   a=sendonly
   a=direction:active
   a=ssrc:222222 cname:active@example.com

This is what KMS would answer:

.. code-block:: text
   :caption: SDP Answer
   :emphasize-lines: 6,9,11,14

   v=0
   o=- 3696336115 3696336115 IN IP4 80.28.30.32
   s=Kurento Media Server
   c=IN IP4 80.28.30.32
   t=0 0
   m=audio 56740 RTP/AVPF 96
   a=rtpmap:96 opus/48000/2
   a=recvonly
   a=direction:passive
   a=ssrc:4061617641 cname:user885892801@host-b546a6e8
   m=video 37616 RTP/AVPF 103
   a=rtpmap:103 H264/90000
   a=recvonly
   a=direction:passive
   a=ssrc:1363449382 cname:user885892801@host-b546a6e8

In this particular example, KMS is installed in a server with the public IP *80.28.30.32*; also, it won't be sending media to the active peer, only receiving it (as requested by the application with ``a=sendonly``, and acknowledged by KMS with ``a=recvonly``).

Note that even in this case, KMS still needs to know on what port the sender is listening for RTCP feedback packets, which are a mandatory part of the RTP protocol. So, in this example, KMS will learn the public IP and port of the active machine, and will use those to send the Receiver Report RTCP packets to the sender.
