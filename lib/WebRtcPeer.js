/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 */

var freeice  = require('freeice');
var inherits = require('inherits');

var EventEmitter = require('events').EventEmitter;

var recursive = require('merge').recursive


function noop(error)
{
  if(error) console.trace(error)
}

function trackStop(track)
{
  track.stop && track.stop()
}


/**
 * @classdesc Wrapper object of an RTCPeerConnection. This object is aimed to
 *            simplify the development of WebRTC-based applications.
 *
 * @constructor module:kurentoUtils.WebRtcPeer
 *
 * @param mode -
 *            {String} Mode in which the PeerConnection will be configured.
 *            Valid values are: 'recv', 'send', and 'sendRecv'
 * @param localVideo -
 *            {Object} Video tag for the local stream
 * @param remoteVideo -
 *            {Object} Video tag for the remote stream
 * @param onsdpoffer -
 *            {Function} Callback executed when a SDP offer has been generated
 * @param onerror -
 *            {Function} Callback executed when an error happens generating an
 *            SDP offer
 * @param videoStream -
 *            {Object} MediaStream to be used as primary source (typically video
 *            and audio, or only video if combined with audioStream) for
 *            localVideo and to be added as stream to the RTCPeerConnection
 * @param audioStream -
 *            {Object} MediaStream to be used as second source (typically for
 *            audio) for localVideo and to be added as stream to the
 *            RTCPeerConnection
 */
function WebRtcPeer(mode, options, callback)
{
  WebRtcPeer.super_.call(this)

  var localVideo, remoteVideo, onsdpoffer, onerror, mediaConstraints;
  var videoStream, audioStream, connectionConstraints;

  while(arguments.length && !arguments[arguments.length-1]) arguments.length--;

  if(arguments.length > 2)  // Deprecated mode
  {
    console.warn('Positional parameters are deprecated for WebRtcPeer')

    localVideo       = arguments[1];
    remoteVideo      = arguments[2];
    onsdpoffer       = arguments[3];
    onerror          = arguments[4];
    mediaConstraints = arguments[5];
    videoStream      = arguments[6];
    audioStream      = arguments[7];
  }
  else
  {
    localVideo       = options.localVideo;
    remoteVideo      = options.remoteVideo;
    onsdpoffer       = options.onsdpoffer;
    onerror          = options.onerror;
    mediaConstraints = options.mediaConstraints;
    videoStream      = options.videoStream;
    audioStream      = options.audioStream;

    connectionConstraints = options.connectionConstraints;
  }

  if(onerror)    this.on('error',    onerror);
  if(onsdpoffer) this.on('sdpoffer', onsdpoffer);


  // Init PeerConnection

  var pc = options.peerConnection
  if(!pc)
  {
    var configuration = recursive(
    {
      iceServers : freeice()
    },
    WebRtcPeer.prototype.server,
    options.configuration);

    pc = new RTCPeerConnection(configuration);
  }

  Object.defineProperty(this, 'peerConnection', {get: function(){return pc;}});

  var self = this;

  var ended = false;
  pc.addEventListener('icecandidate', function(event)
  {
    // candidate exists in event.candidate
    if(event.candidate)
    {
      ended = false;
      return;
    }

    // [Hack] On old Chrome, when there's no Internet connection onicecandidate
    // event is being called infinitelly (bug). This prevent it.
    if(ended) return;
    ended = true;

    self.emit('sdpoffer', pc.localDescription.sdp);
    console.log('ICE negotiation completed');
  });


  //
  // Priviledged methods
  //

  /**
  * @description This method creates the RTCPeerConnection object taking into
  *              account the properties received in the constructor. It starts
  *              the SDP negotiation process: generates the SDP offer and invokes
  *              the onsdpoffer callback. This callback is expected to send the
  *              SDP offer, in order to obtain an SDP answer from another peer.
  *
  * @function module:kurentoUtils.WebRtcPeer.prototype.start
  */
  this.start = function(constraints, callback)
  {
    if(videoStream && localVideo)
    {
      localVideo.src = URL.createObjectURL(videoStream);
      localVideo.muted = true;
    }

    if(videoStream) pc.addStream(videoStream);
    if(audioStream) pc.addStream(audioStream);

    // Adjust arguments

    if(constraints instanceof Function)
    {
      if(callback) throw new Error('Nothing can be defined after the callback')

      callback    = constraints
      constraints = undefined
    }

    constraints = recursive(
    {
      mandatory:
      {
        OfferToReceiveAudio: (mode !== 'sendonly'),
        OfferToReceiveVideo: (mode !== 'sendonly')
      },
      optional:
      [
        {DtlsSrtpKeyAgreement: true}
      ]
    }, constraints);

    callback = callback || noop;


    // Create the offer with the required constrains

    pc.createOffer(function(offer)
    {
      console.log('Created SDP offer');

      pc.setLocalDescription(offer, function()
      {
        console.log('Local description set', offer);

        callback(null, self);
      },
      callback);
    },
    callback, constraints);
  }


  if(mode !== 'recvonly' && !videoStream)
  {
    mediaConstraints = recursive(
    {
      audio: true,
      video:
      {
        mandatory:
        {
          maxWidth: 640,
          maxFrameRate: 15,
          minFrameRate: 15
        }
      }
    }, mediaConstraints);

    getUserMedia(mediaConstraints, function(stream)
    {
      videoStream = stream;

      self.start(options.connectionConstraints, callback)
    },
    callback || noop);
  }
  else
    self.start(options.connectionConstraints, callback)


  /**
  * @description This method frees the resources used by WebRtcPeer.
  *
  * @function module:kurentoUtils.WebRtcPeer.prototype.dispose
  */
  this.dispose = function()
  {
    console.log('Disposing WebRtcPeer');

    // FIXME This is not yet implemented in firefox
    // if(videoStream) pc.removeStream(videoStream);

    // For old browsers, PeerConnection.close() is NOT idempotent and raise
    // error. We check its signaling state and don't close it if it's already
    // closed
    if(pc && pc.signalingState != 'closed') pc.close();

    if(localVideo)  localVideo.src  = '';
    if(remoteVideo) remoteVideo.src = '';

    if(videoStream)
    {
      videoStream.getAudioTracks().forEach(trackStop)
      videoStream.getVideoTracks().forEach(trackStop)
    }

    if(audioStream)
      audioStream.getAudioTracks().forEach(trackStop)
  };

  /**
  * @description Callback function invoked when and SDP answer is received.
  *              Developers are expected to invoke this function in order to
  *              complete the SDP negotiation.
  *
  * @function module:kurentoUtils.WebRtcPeer.prototype.processSdpAnswer
  *
  * @param sdpAnswer -
  *            Description of sdpAnswer
  * @param successCallback -
  *            Called when the remoteDescription and the remoteVideo.src have
  *            been set successfully.
  */
  this.processSdpAnswer = function(sdpAnswer, callback)
  {
    var answer = new RTCSessionDescription(
    {
      type : 'answer',
      sdp : sdpAnswer,
    });

    console.log('SDP answer received, setting remote description');

    callback = callback || noop

    pc.setRemoteDescription(answer, function()
    {
      if(remoteVideo)
      {
        remoteVideo.src = URL.createObjectURL(pc.getRemoteStreams()[0]);

        console.log('Remote URL:',remoteVideo.src)
      }

      callback();
    },
    callback);
  }
}
inherits(WebRtcPeer, EventEmitter)


WebRtcPeer.prototype.getLocalStream = function(index)
{
  if(this.peerConnection)
    return this.peerConnection.getLocalStreams()[index || 0]
}

WebRtcPeer.prototype.getRemoteStream = function(index)
{
  if(this.peerConnection)
    return this.peerConnection.getRemoteStreams()[index || 0]
}


//
// Static factory functions
//

/**
 * @description This method creates the WebRtcPeer object and obtain userMedia
 *              if needed.
 *
 * @function module:kurentoUtils.WebRtcPeer.start
 *
 * @param mode -
 *            {String} Mode in which the PeerConnection will be configured.
 *            Valid values are: 'recv', 'send', and 'sendRecv'
 * @param localVideo -
 *            {Object} Video tag for the local stream
 * @param remoteVideo -
 *            {Object} Video tag for the remote stream
 * @param onSdp -
 *            {Function} Callback executed when a SDP offer has been generated
 * @param onerror -
 *            {Function} Callback executed when an error happens generating an
 *            SDP offer
 * @param mediaConstraints -
 *            {Object[]} Constraints used to create RTCPeerConnection
 * @param videoStream -
 *            {Object} MediaStream to be used as primary source (typically video
 *            and audio, or only video if combined with audioStream) for
 *            localVideo and to be added as stream to the RTCPeerConnection
 * @param videoStream -
 *            {Object} MediaStream to be used as primary source (typically video
 *            and audio, or only video if combined with audioStream) for
 *            localVideo and to be added as stream to the RTCPeerConnection
 * @param audioStream -
 *            {Object} MediaStream to be used as second source (typically for
 *            audio) for localVideo and to be added as stream to the
 *            RTCPeerConnection
 *
 * @return {module:kurentoUtils.WebRtcPeer}
 */
WebRtcPeer.start = function(mode, localVideo, remoteVideo, onsdpoffer, onerror,
    mediaConstraints, videoStream, audioStream, configuration,
    connectionConstraints, callback)
{
  var options =
  {
    localVideo      : localVideo,
    remoteVideo     : remoteVideo,
    onsdpoffer      : onsdpoffer,
    onerror         : onerror,
    mediaConstraints: mediaConstraints,
    videoStream     : videoStream,
    audioStream     : audioStream,
    configuration   : configuration,

    connectionConstraints: connectionConstraints
  };

  return new WebRtcPeer(mode, options, callback);
};

/**
 * @description This methods creates a WebRtcPeer to receive video.
 *
 * @function module:kurentoUtils.WebRtcPeer.startRecvOnly
 *
 * @param remoteVideo -
 *            {Object} Video tag for the remote stream
 * @param onSdp -
 *            {Function} Callback executed when a SDP offer has been generated
 * @param onerror -
 *            {Function} Callback executed when an error happens generating an
 *            SDP offer
 * @param mediaConstraints -
 *            {Object[]} Constraints used to create RTCPeerConnection
 *
 * @return {module:kurentoUtils.WebRtcPeer}
 */
WebRtcPeer.startRecvOnly = function(remoteVideo, onSdp, onError,
  mediaConstraints, configuration, connectionConstraints, callback)
{
  return WebRtcPeer.start('recvonly', null, remoteVideo, onSdp, onError,
      mediaConstraints, null, null, configuration, connectionConstraints,
      callback);
};

/**
 * @description This methods creates a WebRtcPeer to send video.
 *
 * @function module:kurentoUtils.WebRtcPeer.startSendOnly
 *
 * @param localVideo -
 *            {Object} Video tag for the local stream
 * @param onSdp -
 *            {Function} Callback executed when a SDP offer has been generated
 * @param onerror -
 *            {Function} Callback executed when an error happens generating an
 *            SDP offer
 * @param mediaConstraints -
 *            {Object[]} Constraints used to create RTCPeerConnection
 *
 * @return {module:kurentoUtils.WebRtcPeer}
 */
WebRtcPeer.startSendOnly = function(localVideo, onSdp, onError,
  mediaConstraints, configuration, connectionConstraints, callback)
{
  return WebRtcPeer.start('sendonly', localVideo, null, onSdp, onError,
      mediaConstraints, null, null, configuration, connectionConstraints,
      callback);
};

/**
 * @description This methods creates a WebRtcPeer to send and receive video.
 *
 * @function module:kurentoUtils.WebRtcPeer.startSendRecv
 *
 * @param localVideo -
 *            {Object} Video tag for the local stream
 * @param remoteVideo -
 *            {Object} Video tag for the remote stream
 * @param onSdp -
 *            {Function} Callback executed when a SDP offer has been generated
 * @param onerror -
 *            {Function} Callback executed when an error happens generating an
 *            SDP offer
 * @param mediaConstraints -
 *            {Object[]} Constraints used to create RTCPeerConnection
 *
 * @return {module:kurentoUtils.WebRtcPeer}
 */
WebRtcPeer.startSendRecv = function(localVideo, remoteVideo, onSdp, onError,
  mediaConstraints, configuration, connectionConstraints, callback)
{
  return WebRtcPeer.start('sendrecv', localVideo, remoteVideo, onSdp,
      onError, mediaConstraints, null, null, configuration,
      connectionConstraints, callback);
};


//
// Specialized child classes
//

function WebRtcPeerRecvonly(options)
{
  WebRtcPeerRecvonly.super_.call(this, 'recvonly', options)
}
inherits(WebRtcPeerRecvonly, WebRtcPeer)

function WebRtcPeerSendonly(options)
{
  WebRtcPeerSendonly.super_.call(this, 'sendonly', options)
}
inherits(WebRtcPeerSendonly, WebRtcPeer)

function WebRtcPeerSendrecv(options)
{
  WebRtcPeerSendrecv.super_.call(this, 'sendrecv', options)
}
inherits(WebRtcPeerSendrecv, WebRtcPeer)


module.exports = WebRtcPeer;

WebRtcPeer.WebRtcPeer         = WebRtcPeer;
WebRtcPeer.WebRtcPeerRecvonly = WebRtcPeerRecvonly;
WebRtcPeer.WebRtcPeerSendonly = WebRtcPeerSendonly;
WebRtcPeer.WebRtcPeerSendrecv = WebRtcPeerSendrecv;
