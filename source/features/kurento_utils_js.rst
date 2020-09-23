%%%%%%%%%%%%%%%%
Kurento Utils JS
%%%%%%%%%%%%%%%%

[TODO full review]

Overview
========

Kurento Utils is a wrapper object of an
`RTCPeerConnection <https://w3c.github.io/webrtc-pc/>`__. This object is aimed
to simplify the development of WebRTC-based applications.

The source code of this project can be cloned from the
`GitHub repository <https://github.com/kurento/kurento-utils>`__.


How to use it
=============

* **Minified file** - Download the file from
  `here <http://builds.openvidu.io/release/|VERSION_UTILS_JS|/js/kurento-utils.min.js>`__.


* **NPM** - Install and use library in your NodeJS files.

  .. sourcecode:: bash

     npm install kurento-utils


  .. sourcecode:: javascript

     var utils = require('kurento-utils');

* **Bower** - Generate the bundled script file

  .. sourcecode:: bash

     bower install kurento-utils

  Import the library in your *html* page

  .. sourcecode:: html

     <script
     src="bower_components/kurento-utils/js/kurento-utils.js"></script>

Examples
========

There are several tutorials that show kurento-utils used in complete WebRTC
applications developed on Java, Node and JavaScript. These tutorials are in
GitHub, and you can download and run them at any time.

* **Java** - https://github.com/Kurento/kurento-tutorial-java

* **Node** - https://github.com/Kurento/kurento-tutorial-node

* **JavaScript** - https://github.com/Kurento/kurento-tutorial-js


In the following lines we will show how to use the library to create an
*RTCPeerConnection*, and how to negotiate the connection with another peer.
The library offers a *WebRtcPeer* object, which is a wrapper of the browser's
RTCPeerConnection API. Peer connections can be of different types:
unidirectional (send or receive only) or bidirectional (send and receive). The
following code shows how to create the latter, in order to be able to send and
receive media (audio and video). The code assumes that there are two video tags
in the page that loads the script. These tags will be used to show the video as
captured by your own client browser, and the media received from the other
peer. The constructor receives a property that holds all the information needed
for the configuration.

.. sourcecode:: javascript

    var videoInput = document.getElementById('videoInput');
    var videoOutput = document.getElementById('videoOutput');

    var constraints = {
        audio: true,
        video: {
          width: 640,
          framerate: 15
        }
    };

    var options = {
      localVideo: videoInput,
      remoteVideo: videoOutput,
      onicecandidate : onIceCandidate,
      mediaConstraints: constraints
    };


   var webRtcPeer = kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options, function(error) {
         if(error) return onError(error)

         this.generateOffer(onOffer)
      });

With this little code, the library takes care of creating the
*RTCPeerConnection*, and invoking *getUserMedia* in the browser if needed.
The constraints in the property are used in the invocation, and in this case
both microphone and webcam will be used. However, this does not create the
connection. This is only achieved after completing the SDP negotiation between
peers. This process implies exchanging SDPs offer and answer and, since
:term:`Trickle ICE` is used, a number of candidates describing the capabilities
of each peer. How the negotiation works is out of the scope of this document.
More info can be found in
`this <https://tools.ietf.org/id/draft-nandakumar-rtcweb-sdp-01.html>`__ link.

In the previous piece of code, when the *webRtcPeer* object gets created, the
SDP offer is generated with ``this.generateOffer(onOffer)``. The only argument
passed is a function, that will be invoked one the browser's peer connection
has generated that offer. The *onOffer* callback method is responsible for
sending this offer to the other peer, by any means devised in your application.
Since that is part of the signaling plane and business logic of each particular
application, it won't be covered in this document.

Assuming that the SDP offer has been received by the remote peer, it must have
generated an SDP answer, that should be received in return. This answer must be
processed by the *webRtcEndpoint*, in order to fulfill the negotiation. This
could be the implementation of the *onOffer* callback function. We've assumed
that there's a function somewhere in the scope, that allows sending the SDP to
the remote peer.

.. sourcecode:: javascript

  function onOffer(error, sdpOffer) {
    if (error) return onError(error);

    // We've made this function up sendOfferToRemotePeer(sdpOffer,
    function(sdpAnswer) {
      webRtcPeer.processAnswer(sdpAnswer);
    });
  }

As we've commented before, the library assumes the use of :term:`Trickle ICE` to
complete the connection between both peers. In the configuration of the
*webRtcPeer*, there is a reference to a *onIceCandidate* callback function.
The library will use this function to send ICE candidates to the remote peer.
Since this is particular to each application, we will just show the signature

.. sourcecode:: javascript

  function onIceCandidate(candidate) {
    // Send the candidate to the remote peer
  }

In turn, our client application must be able to receive ICE candidates from the
remote peer. Assuming the signaling takes care of receiving those candidates,
it is enough to invoke the following method in the *webRtcPeer* to consider
the ICE candidate

.. sourcecode:: javascript

       webRtcPeer.addIceCandidate(candidate);

Following the previous steps, we have:

* Sent an SDP offer to a remote peer

* Received an SDP answer from the remote peer, and have the *webRtcPeer*
  process that answer.

* Exchanged ICE candidates between both peer, by sending the ones generated in
  the browser, and processing the candidates received by the remote peer.


This should complete the negotiation process, and should leave us with a working
bidirectional WebRTC media exchange between both peers.

Using data channels
===================

WebRTC data channels lets you send text or binary data over an active WebRTC connection. The **WebRtcPeer** object can provide access to this functionality by using the `RTCDataChannel <https://developer.mozilla.org/en-US/docs/Games/Techniques/WebRTC_data_channels>`__ form the wrapped **RTCPeerConnection** object. This allows you to inject into and consume data from the pipeline. This data can be treated by each endpoint differently. For instance, a *WebRtcPeer* object in the browser, will have the same behavior as the *RTCDataChannel* (you can see a description `here <https://developer.mozilla.org/en-US/docs/Web/API/WebRTC_API/WebRTC_basics#DataChannel>`__). Other endpoints could make use of this channel to send information: a filter that detects QR codes in a video stream, could send the detected code to the clients through a data channel. This special behavior should be specified in the filter.

The use of data channels in the *WebRtcPeer* object is indicated by passing the *dataChannels* flag in the options bag, along with the desired options.

.. sourcecode:: javascript
   :emphasize-lines: 4-12

    var options = {
        localVideo : videoInput,
        remoteVideo : videoOutput,
        dataChannels : true,
        dataChannelConfig: {
          id : getChannelName(),
          onmessage : onMessage,
          onopen : onOpen,
          onclose : onClosed,
          onbufferedamountlow : onbufferedamountlow,
          onerror : onerror
        },
        onicecandidate : onIceCandidate
    }

    webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options, onWebRtcPeerCreated);

The values in *dataChannelConfig* are all optional. Once the *webRtcPeer* object is created, and after the connection has been successfully negotiated, users can send data through the data channel

.. sourcecode:: javascript

    webRtcPeer.send('your data stream here');

The format of the data you are sending, is determined by your application, and the definition of the endpoints that you are using.

The lifecycle of the underlying *RTCDataChannel*, is tied to that of the *webRtcPeer*: when the ``webRtcPeer.dispose()`` method is invoked, the data channel will be closed and released too.


Reference documentation
=======================

WebRtcPeer
**********

The constructor for WebRtcPeer is WebRtcPeer(**mode, options, callback**) where:

* **mode**: Mode in which the PeerConnection will be configured. Valid values
  are

   * *recv*: receive only media.
   * *send*: send only media.
   * *sendRecv*: send and receive media.

* **options** : It is a group of parameters and they are optional. It is a
  json object.

   * *localVideo*: Video tag in the application for the local stream.
   * *remoteVideo*: Video tag in the application for the remote stream.
   * *videoStream*: Provides an already available video stream that will
     be used instead of using the media stream from the local webcam.
   * *audioStreams*: Provides an already available audio stream that will
     be used instead of using the media stream from the local microphone.
   * *mediaConstraints*: Defined the quality for the video and audio
   * *peerConnection*: Use a peerConnection which was created before
   * *sendSource*: Which source will be used

      * *webcam*
      * *screen*
      * *window*
   * *onstreamended*: Method that will be invoked when stream ended event
     happens
   * *onicecandidate*: Method that will be invoked when ice candidate event
     happens
   * *oncandidategatheringdone*: Method that will be invoked when all
     candidates have been harvested
   * *dataChannels*: Flag for enabling the use of data channels. If *true*, then a data channel will be created in the *RTCPeerConnection* object.
   * *dataChannelConfig*: It is a JSON object with the configuration passed to the DataChannel when created. It supports the following keys:

      * *id*: Specifies the *id* of the data channel. If none specified, the same *id* of the *WebRtcPeer* object will be used.
      * *options*: Options object passed to the data channel constructor.
      * *onopen*: Function invoked in the *onopen* event of the data channel, fired when the channel is open.
      * *onclose*: Function invoked in the *onclose* event of the data channel, fired when the data channel is closed.
      * *onmessage*: Function invoked in the *onmessage* event of the data channel. This event is fired every time a message is received.
      * *onbufferedamountlow*: Is the event handler called when the *bufferedamountlow* event is received. Such an event is sent when ``RTCDataChannel.bufferedAmount`` drops to less than or equal to the amount specified by the ``RTCDataChannel.bufferedAmountLowThreshold`` property.
      * *onerror*: Callback function onviked when an error in the data channel is produced. If none is provided, an error trace message will be logged in the browser console.
   * *simulcast*: Indicates whether simulcast is going to be used. Value is
     *true|false*
   * *configuration*: It is a JSON object where ICE Servers are defined
     using

      * `iceServers <https://w3c.github.io/webrtc-pc/#idl-def-RTCIceServer>`__:
        The format for this variable is like::

               [{"urls":"turn:turn.example.org","username":"user","credential":"myPassword"}]
               [{"urls":"stun:stun1.example.net"},{"urls":"stun:stun2.example.net"}]

* **callback**: It is a callback function which indicate, if all worked right
  or not


Also there are 3 specific methods for creating WebRtcPeer objects without using
*mode* parameter:

   * **WebRtcPeerRecvonly(options, callback)**: Create a WebRtcPeer as
     receive only.
   * **WebRtcPeerSendonly(options, callback)**: Create a WebRtcPeer as send
     only.
   * **WebRtcPeerSendrecv(options, callback)**: Create a WebRtcPeer as send
     and receive.

MediaConstraints
----------------

Constraints provide a general control surface that allows applications to both
select an appropriate source for a track and, once selected, to influence how a
source operates. ``getUserMedia()`` uses constraints to help select an
appropriate source for a track and configure it. For more information about
media constraints and its values, you can check
`here  <https://www.w3.org/TR/mediacapture-streams/>`__.

By default, if the mediaConstraints is undefined, this constraints are used when
*getUserMedia* is called::

   {
     audio: true,
     video: {
       width: 640,
       framerate: 15
     }
   }

If *mediaConstraints* has any value, the library uses this value for the
invocation of *getUserMedia*. It is up to the browser whether those
constraints are accepted or not.

In the examples section, there is one example about the use of media constraints.

Methods
-------

getPeerConnection
`````````````````

Using this method the user can get the peerConnection and use it directly.

showLocalVideo
``````````````

Use this method for showing the local video.

getLocalStream
``````````````

Using this method the user can get the local stream. You can use **muted**
property to silence the audio, if this property is *true*.

getRemoteStream
```````````````

Using this method the user can get the remote stream.

getCurrentFrame
```````````````

Using this method the user can get the current frame and get a canvas with an
image of the current frame.

processAnswer
`````````````

Callback function invoked when a SDP answer is received. Developers are expected
to invoke this function in order to complete the SDP negotiation. This method
has two parameters:

* **sdpAnswer**: Description of sdpAnswer
* **callback**: It is a function with *error* like parameter. It is called
  when the remote description has been set successfully.

processOffer
````````````

Callback function invoked when a SDP offer is received. Developers are expected
to invoke this function in order to complete the SDP negotiation. This method
has two parameters:

* **sdpOffer**: Description of sdpOffer
* **callback**: It is a function with *error* and *sdpAnswer* like parameters.
  It is called when the remote description has been set successfully.

dispose
```````

This method frees the resources used by WebRtcPeer.

addIceCandidate
```````````````

Callback function invoked when an ICE candidate is received. Developers are
expected to invoke this function in order to complete the SDP negotiation. This
method has two parameters:

* **iceCandidate**: Literal object with the ICE candidate description
* **callback**: It is a function with *error* like parameter. It is called
  when the ICE candidate has been added.

getLocalSessionDescriptor
`````````````````````````

Using this method the user can get peerconnection's local session descriptor.

getRemoteSessionDescriptor
``````````````````````````

Using this method the user can get peerconnection's remote session descriptor.

generateOffer
`````````````

Creates an offer that is a request to find a remote peer with a specific
configuration.


How to do screen share
**********************

Screen and window sharing depends on the privative module
*kurento-browser-extensions*. To enable its support, you'll need to install the
package dependency manually or provide a *getScreenConstraints* function
yourself on runtime. The option **sendSource** could be *window* or *screen*
before create a WebRtcEndpoint. If it's not available, when trying to share the
screen or a window content it will throw an exception.

Souce code
==========

The code is at `github <https://github.com/kurento/kurento-utils-js>`__.

Be sure to have :term:`Node.js` and :term:`Bower` installed in your system:

.. sourcecode:: bash

   curl -sL https://deb.nodesource.com/setup_8.x | sudo -E bash -
   sudo apt-get install -y nodejs
   sudo npm install -g bower

To install the library, it is recommended to do that from the
`NPM repository <https://www.npmjs.org/package/kurento-utils>`__:

.. sourcecode:: bash

   npm install kurento-utils

Alternatively, you can download the code using Git and install manually its
dependencies:

.. sourcecode:: bash

   git clone https://github.com/Kurento/kurento-utils
   cd kurento-utils
   npm install


Build for browser
=================

After you download the project, to build the browser version of the library
you'll only need to execute the `grunt <https://gruntjs.com/>`__ task runner. The
file needed will be generated on the *dist* folder. Alternatively, if you don't
have it globally installed, you can run a local copy by executing:

.. sourcecode:: bash

   cd kurento-utils
   node_modules/.bin/grunt
