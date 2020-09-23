===============
Endpoint Events
===============

This is a list of all events that can be emitted by an instance of *WebRtcEndpoint*. This class belongs to a chain of inherited classes, so this list includes events from all of them, starting from the topmost class in the inheritance tree:

.. contents:: Table of Contents



MediaObject events
==================

This is the base interface used to manage capabilities common to all Kurento elements, including both *MediaElement* and *MediaPipeline*.



Error
-----

Some error has occurred. Check the event parameters (such as *description*, *errorCode*, and *type*) to get information about what happened.



.. _events-mediaelement:

MediaElement events
===================

These events indicate some low level information about the state of `GStreamer <https://gstreamer.freedesktop.org>`__, the underlying multimedia framework.



ElementConnected
----------------

[TODO - add contents]



ElementDisconnected
-------------------

[TODO - add contents]



MediaFlowInStateChange
----------------------

- State = *Flowing*: Data is arriving from the KMS Pipeline, and **flowing into** the Element. Technically, this means that there are GStreamer Buffers flowing from the Pipeline to the Element's *sink* pad. For example, with a Recorder element this event would fire when media arrives from the Pipeline to be written to disk.

- State = *NotFlowing*: The Element is not receiving any input data from the Pipeline.



MediaFlowOutStateChange
-----------------------

- State = *Flowing*: There is data **flowing out** from the Element towards the KMS Pipeline. Technically, this means that there are GStreamer Buffers flowing from the Element's *src* pad to the Pipeline. For example, with a Player element this event would fire when media is read from disk and is pushed to the Pipeline.

- State = *NotFlowing*: The Element is not sending any output data to the Pipeline.



MediaTranscodingStateChange
---------------------------

All Endpoint objects in Kurento Media Server embed a custom-made GStreamer element called `agnosticbin`. This element is used to provide seamless interconnection of components in the *MediaPipeline*, regardless of the format and codec configuration of the input and output media streams.

When media starts flowing through any *MediaElement*-derived object, an internal dynamic configuration is **automatically done** in order to match the incoming and outgoing media formats. If both input and output formats are compatible (at the codec level), then the media can be transferred directly without any extra processing. However, if the input and output media formats don't match, the internal transcoding module will get enabled to convert between them.

For example: If a *WebRtcEndpoint* receives a *VP8* video stream from a Chrome browser, and has to send it to a Safari browser (which only supports the *H.264* codec), then the media needs to be transcoded. The *WebRtcEndpoint* will automatically do it.

- State = *Transcoding*: The *MediaElement* will transcode the incoming media, because its format is not compatible with the requested output.

- State = *NotTranscoding*: The *MediaElement* will *not* transcode the incoming media, because its format is compatible with the requested output.



.. _events-basertpendpoint:

BaseRtpEndpoint events
======================

These events provide information about the state of the RTP connection for each stream in the WebRTC call.

Note that the *MediaStateChanged* event is not 100% reliable to check if a RTP connection is active: RTCP packets do not usually flow at a constant rate. For example, minimizing a browser window with an *RTCPeerConnection* might affect this interval.



ConnectionStateChanged
----------------------

- State = *Connected*: All of the *KmsIRtpConnection* objects have been created [TODO: explain what this means].

- State = *Disconnected*: At least one of the *KmsIRtpConnection* objects is not created yet.

Call sequence:

.. code-block:: text

   signal KmsIRtpConnection::"connected"
     -> signal KmsSdpSession::"connection-state-changed"
       -> signal KmsBaseRtpEndpoint::"connection-state-changed"
         -> BaseRtpEndpointImpl::updateConnectionState



MediaStateChanged
-----------------

- State = *Connected*: At least *one* of the audio or video RTP streams in the session is still alive (sending or receiving RTCP packets). Equivalent to the signal `GstRtpBin::"on-ssrc-active" <https://gstreamer.freedesktop.org/data/doc/gstreamer/head/gst-plugins-good/html/gst-plugins-good-plugins-rtpbin.html#GstRtpBin-on-ssrc-active>`__, which gets triggered whenever the GstRtpBin receives an *RTCP Sender Report* (*RTCP SR*) or *RTCP Receiver Report* (*RTCP RR*).

- State = *Disconnected*: None of the RTP streams belonging to the session is alive (ie. no RTCP packets are sent or received for any of them).

These signals from `GstRtpBin`_ will trigger the *MediaStateChanged* event:

- ``GstRtpBin::"on-bye-ssrc"``: State = *Disconnected*.
- ``GstRtpBin::"on-bye-timeout"``: State = *Disconnected*.
- ``GstRtpBin::"on-timeout"``: State = *Disconnected*.
- ``GstRtpBin::"on-ssrc-active"``: State = *Connected*.

.. _GstRtpBin: https://gstreamer.freedesktop.org/data/doc/gstreamer/head/gst-plugins-good/html/gst-plugins-good-plugins-rtpbin.html

Call sequence:

.. code-block:: text

   signal GstRtpBin::"on-bye-ssrc"
   || signal GstRtpBin::"on-bye-timeout"
   || signal GstRtpBin::"on-timeout"
   || signal GstRtpBin::"on-ssrc-active"
     -> signal KmsBaseRtpEndpoint::"media-state-changed"
       -> BaseRtpEndpointImpl::updateMediaState

.. note::

   *MediaStateChanged* (State = *Connected*) will happen after these other events have been emitted:

   1. *NewCandidatePairSelected*.
   2. *IceComponentStateChanged* (State: *Connected*).
   3. *MediaFlowOutStateChange* (State: *Flowing*).



WebRtcEndpoint events
=====================

These events provide information about the state of `libnice <https://nice.freedesktop.org>`__, the underlying library in charge of the ICE Gathering process. The ICE Gathering is typically done before attempting any WebRTC call.

For further reference, see the libnice's `Agent documentation <https://nice.freedesktop.org/libnice/NiceAgent.html>`__ and `source code <https://cgit.freedesktop.org/libnice/libnice/tree/agent/agent.h>`__.



DataChannelClose
----------------

[TODO - add contents]



DataChannelOpen
---------------

[TODO - add contents]



IceCandidateFound
-----------------

A new local candidate has been found, after the ICE Gathering process was started. Equivalent to the signal `NiceAgent::"new-candidate-full" <https://nice.freedesktop.org/libnice/NiceAgent.html#NiceAgent-new-candidate-full>`__.



.. _events-icecomponentstatechange:

IceComponentStateChange
-----------------------

This event carries the state values from the signal `NiceAgent::"component-state-changed" <https://nice.freedesktop.org/libnice/NiceAgent.html#NiceAgent-component-state-changed>`__.

- State = *Disconnected*: There is no active connection, and the ICE process is idle.

  NiceAgent state: *NICE_COMPONENT_STATE_DISCONNECTED*, "*No activity scheduled*".

- State = *Gathering*: The Endpoint has started finding all possible local candidates, which will be notified through the event *IceCandidateFound*.

  NiceAgent state: *NICE_COMPONENT_STATE_GATHERING*, "*Gathering local candidates*".

- State = *Connecting*: The Endpoint has started the connectivity checks between **at least** one pair of local and remote candidates. These checks will always start as soon as possible (i.e. whenever the very first remote candidates arrive), so don't assume that the candidate gathering has already finished, because it will probably still be running in parallel; some (possibly better) candidates might still be waiting to be found and gathered.

  NiceAgent state: *NICE_COMPONENT_STATE_CONNECTING*, "*Establishing connectivity*".

- State = *Connected*: **At least** one candidate pair resulted in a successful connection. This happens right after the event *NewCandidatePairSelected*. When this event triggers, the effective communication between peers can start, and usually this means that media will start flowing between them. However, the candidate gathering hasn't really finished yet, which means that some (possibly better) candidates might still be waiting to be found, gathered, checked for connectivity, and if that completes successfully, selected as new candidate pair.

  NiceAgent state: *NICE_COMPONENT_STATE_CONNECTED*, "*At least one working candidate pair*".

- State = *Ready*: All local candidates have been gathered, all pairs of local and remote candidates have been tested for connectivity, and a successful connection was established.

  NiceAgent state: *NICE_COMPONENT_STATE_READY*, "*ICE concluded, candidate pair selection is now final*".

- State = *Failed*: All local candidates have been gathered, all pairs of local and remote candidates have been tested for connectivity, but still none of the connection checks was successful, so no connectivity was reached to the remote peer.

  NiceAgent state: *NICE_COMPONENT_STATE_FAILED*, "*Connectivity checks have been completed, but connectivity was not established*".

This graph shows the possible state changes (`source <https://cgit.freedesktop.org/libnice/libnice/tree/docs/reference/libnice/states.gv>`__):

.. graphviz:: /images/graphs/events-libnice-states.dot
   :align: center
   :caption: libnice state transition diagram for NiceComponentState

.. note::

   The states *Ready* and *Failed* indicate that the ICE transport has completed gathering and is currently idle. However, since events such as adding a new interface or a new :term:`STUN`/:term:`TURN` server will cause the state to go back, *Ready* and *Failed* are **not** terminal states.



IceGatheringDone
----------------

All local candidates have been found, so the gathering process is finished for this peer. Note this doesn't imply that the remote peer has finished its own gathering, so more remote candidates might still arrive. Equivalent to the signal `NiceAgent::"candidate-gathering-done" <https://nice.freedesktop.org/libnice/NiceAgent.html#NiceAgent-candidate-gathering-done>`__.



.. _events-newcandidatepairselected:

NewCandidatePairSelected
------------------------

During the connectivity checks one of the pairs happened to provide a successful connection, and the pair had a higher preference than the previously selected one (or there was no previously selected pair yet). Equivalent to the signal `NiceAgent::"new-selected-pair" <https://nice.freedesktop.org/libnice/NiceAgent.html#NiceAgent-new-selected-pair-full>`__.



Sample sequence of events: WebRtcEndpoint
=========================================

Once an instance of *WebRtcEndpoint* is created inside a Media Pipeline, an event handler should be added for each one of the events that can be emitted by the endpoint. Later, the endpoint should be instructed to do one of either:

- Generate an SDP Offer, when KMS is the caller. Later, the remote peer will generate an SDP Answer as a reply, which must be provided to the endpoint.

- Process an SDP Offer generated by the remote peer, when KMS is the callee. This will in turn generate an SDP Answer, which should be provided to the remote peer.

As a last step, the *WebRtcEndpoint* should be instructed to start the ICE Gathering process.

You can see a working example of this in :doc:`/tutorials/java/tutorial-helloworld`. This example code shows the typical usage for the *WebRtcEndpoint*:

.. code-block:: java

    KurentoClient kurento;
    MediaPipeline pipeline = kurento.createMediaPipeline();
    WebRtcEndpoint webRtcEp = new WebRtcEndpoint.Builder(pipeline).build();
    webRtcEp.addIceCandidateFoundListener(...);
    webRtcEp.addIceComponentStateChangedListener(...);
    webRtcEp.addIceGatheringDoneListener(...);
    webRtcEp.addNewCandidatePairSelectedListener(...);

    // Receive an SDP Offer, via the application's custom signaling mechanism
    String sdpOffer = recvMessage();

    // Process the SDP Offer, generating an SDP Answer
    String sdpAnswer = webRtcEp.processOffer(sdpOffer);

    // Send the SDP Answer, via the application's custom signaling mechanism
    sendMessage(sdpAnswer);

    // Start gathering candidates for ICE
    webRtcEp.gatherCandidates();

The application's custom signaling mechanism could be as simple as some ad-hoc messaging protocol built upon WebSocket endpoints.

When a *WebRtcEndpoint* instance has been created, and all event handlers have been added, starting the ICE process will generate a sequence of events very similar to this one:

.. code-block:: text

   IceCandidateFound
   IceComponentStateChanged (Gathering)
   AddIceCandidate
   IceComponentStateChanged (Connecting)
   AddIceCandidate
   IceCandidateFound
   NewCandidatePairSelected
   IceComponentStateChanged (Connected)
   NewCandidatePairSelected
   IceGatheringDone
   IceComponentStateChanged: (Ready)

1. *IceCandidateFound*

   Repeated multiple times; tipically, candidates of type *host* (corresponding to the LAN, local network) are almost immediately found after starting the ICE gathering, and this event can arrive even before the event *IceComponentStateChanged* is emitted.

2. *IceComponentStateChanged* (state: *Gathering*)

   At this point, the local peer is gathering more candidates, and it is also waiting for the candidates gathered by the remote peer, which could start arriving at any time.

3. *AddIceCandidate*

   Repeated multiple times; the remote peer found some initial candidates, and started sending them. Typically, the first candidate received is of type *host*, because those are found the fastest.

4. *IceComponentStateChanged* (state: *Connecting*)

   After receiving the very first of the remote candidates, the ICE Agent starts with the connectivity checks.

5. *AddIceCandidate*

   Repeated multiple times; the remote peer will continue sending its own gathered candidates, of any type: *host*, *srflx* (:term:`STUN`), *relay* (:term:`TURN`).

6. *IceCandidateFound*

   Repeated multiple times; the local peer will also continue finding more of the available local candidates.

7. *NewCandidatePairSelected*

   The ICE Agent makes local and remote candidate pairs. If one of those pairs pass the connectivity checks, it is selected for the WebRTC connection.

8. *IceComponentStateChanged* (state: *Connected*)

   After selecting a candidate pair, the connection is established. *At this point, the media stream(s) can start flowing*.

9. *NewCandidatePairSelected*

   Typically, better candidate pairs will be found over time. The old pair will be abandoned in favor of the new one.

10. *IceGatheringDone*

    When all candidate pairs have been tested, no more work is left to do for the ICE Agent. The gathering process is finished.

11. *IceComponentStateChanged* (state: *Ready*)

    As a consequence of finishing the ICE gathering, the component state gets updated.
