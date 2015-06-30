/*
 * (C) Copyright 2014-2015 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License (LGPL)
 * version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

var freeice = require('freeice')
var inherits = require('inherits')
var UAParser = require('ua-parser-js')
var uuid = require('uuid');

var EventEmitter = require('events').EventEmitter

var recursive = require('merge').recursive.bind(undefined, true)

try {
  require('kurento-browser-extensions')
} catch (error) {
  if (typeof getScreenConstraints === 'undefined') {
    console.warn('screen sharing is not available')

    getScreenConstraints = function getScreenConstraints(sendSource, callback) {
      callback(new Error("This library is not enabled for screen sharing"))
    }
  }
}

var MEDIA_CONSTRAINTS = {
  audio: true,
  video: {
    mandatory: {
      maxWidth: 640,
      maxFrameRate: 15,
      minFrameRate: 15
    }
  }
}

// Somehow, the UAParser constructor gets an empty window object.
// We need to pass the user agent string in order to get information
var ua = (window && window.navigator) ? window.navigator.userAgent : ''
var parser = new UAParser(ua)

function noop(error) {
  if (error) console.error(error)
}

function trackStop(track) {
  track.stop && track.stop()
}

function streamStop(stream) {
  stream.getTracks().forEach(trackStop)
}

function bufferizeCandidates(pc, onerror) {
  var candidatesQueue = []

  function onsignalingstatechange() {
    if (this.signalingState === 'stable')
      while (candidatesQueue.length) {
        var entry = candidatesQueue.shift()

        this.addIceCandidate(entry.candidate, entry.callback, entry.callback)
      }

    this.removeEventListener('signalingstatechange', onsignalingstatechange)
  }

  pc.addEventListener('signalingstatechange', onsignalingstatechange)

  return function (candidate, callback) {
    callback = callback || onerror

    switch (pc.signalingState) {
    case 'closed':
      callback(new Error('PeerConnection object is closed'))
      break
    case 'stable':
      // PeerConnection objects are initially on 'stable' state, but we can only
      // add them the ICE candidates after they have a remote description, so if
      // we don't have it, we queue the ICE candidates until the PeerConnection
      // objects are ready
      if (pc.remoteDescription) {
        pc.addIceCandidate(candidate, callback, callback)
        break
      }
    default:
      candidatesQueue.push({
        candidate: candidate,
        callback: callback
      })
    }
  }
}

/**
 * Wrapper object of an RTCPeerConnection. This object is aimed to simplify the
 * development of WebRTC-based applications.
 *
 * @constructor module:kurentoUtils.WebRtcPeer
 *
 * @param {String} mode Mode in which the PeerConnection will be configured.
 *  Valid values are: 'recv', 'send', and 'sendRecv'
 * @param localVideo Video tag for the local stream
 * @param remoteVideo Video tag for the remote stream
 * @param {MediaStream} videoStream Stream to be used as primary source
 *  (typically video and audio, or only video if combined with audioStream) for
 *  localVideo and to be added as stream to the RTCPeerConnection
 * @param {MediaStream} audioStream Stream to be used as second source
 *  (typically for audio) for localVideo and to be added as stream to the
 *  RTCPeerConnection
 */
function WebRtcPeer(mode, options, callback) {
  if (!(this instanceof WebRtcPeer)) {
    return new WebRtcPeer(mode, options, callback)
  }

  WebRtcPeer.super_.call(this)

  if (options instanceof Function) {
    callback = options
    options = undefined
  }

  options = options || {}
  callback = (callback || noop).bind(this)

  var localVideo = options.localVideo
  var remoteVideo = options.remoteVideo
  var videoStream = options.videoStream
  var audioStream = options.audioStream
  var mediaConstraints = options.mediaConstraints

  var connectionConstraints = options.connectionConstraints
  var sendSource = options.sendSource || 'webcam'

  var guid = uuid.v4()
  var configuration = recursive({
      iceServers: freeice()
    },
    options.configuration)

  var onstreamended = options.onstreamended
  if (onstreamended) this.on('streamended', onstreamended)

  var onicecandidate = options.onicecandidate
  if (onicecandidate) this.on('icecandidate', onicecandidate)

  var oncandidategatheringdone = options.oncandidategatheringdone
  if (oncandidategatheringdone) {
    this.on('candidategatheringdone', oncandidategatheringdone)
  }

  var newPc // Ugly hack, should use a RPC mechanism
  var pc = options.peerConnection

  Object.defineProperties(this, {
    'peerConnection': {
      get: function () {
        return pc
      }
    },

    'remoteVideo': {
      get: function () {
        return remoteVideo
      }
    },

    'localVideo': {
      get: function () {
        return localVideo
      }
    },

    /**
     * @member {(external:ImageData|undefined)} currentFrame
     */
    'currentFrame': {
      get: function () {
        // [ToDo] Find solution when we have a remote stream but we didn't set
        // a remoteVideo tag
        if (!remoteVideo) return;

        if (remoteVideo.readyState < remoteVideo.HAVE_CURRENT_DATA)
          throw new Error('No video stream data available')

        var canvas = document.createElement('canvas')
        canvas.width = remoteVideo.videoWidth
        canvas.height = remoteVideo.videoHeight

        canvas.getContext('2d').drawImage(remoteVideo, 0, 0)

        return canvas
      }
    }
  })

  var self = this

  function streamEndedListener() {
    self.emit('streamended', this);
  };

  var addIceCandidate

  function createPeerConnection(configuration) {
    var pc = new RTCPeerConnection(configuration);

    addIceCandidate = bufferizeCandidates(pc)

    var candidatesQueueOut = []

    self.on('newListener', function (event, listener) {
      if (event === 'icecandidate' || event === 'candidategatheringdone')
        while (candidatesQueueOut.length) {
          var candidate = candidatesQueueOut.shift()

          if (!candidate === (event === 'candidategatheringdone'))
            listener(candidate)
        }
    })

    function onicecandidate(event) {
      var candidate = event.candidate

      if (EventEmitter.listenerCount(self, 'icecandidate') || EventEmitter.listenerCount(
          self, 'candidategatheringdone')) {
        if (candidate)
          self.emit('icecandidate', candidate);
        else
          self.emit('candidategatheringdone');
      }

      // Not listening to 'icecandidate' or 'candidategatheringdone' events, queue
      // the candidate until one of them is listened
      else
        candidatesQueueOut.push(candidate)

      if (!candidate)
        this.removeEventListener('icecandidate', onicecandidate);
    }

    pc.addEventListener('icecandidate', onicecandidate);
    pc.addEventListener('iceconnectionstatechange', oniceconnectionstatechange)

    if (videoStream) {
      videoStream.addEventListener('ended', streamEndedListener);
      pc.addStream(videoStream);
    }

    if (audioStream) {
      audioStream.addEventListener('ended', streamEndedListener);
      pc.addStream(audioStream);
    }

    return pc
  }

  //
  // Priviledged methods
  //

  /**
   * Callback function invoked when an ICE candidate is received. Developers are
   * expected to invoke this function in order to complete the SDP negotiation.
   *
   * @function module:kurentoUtils.WebRtcPeer.prototype.addIceCandidate
   *
   * @param iceCandidate - Literal object with the ICE candidate description
   * @param callback - Called when the ICE candidate has been added.
   */
  this.addIceCandidate = function (iceCandidate, callback) {
    var candidate = new RTCIceCandidate(iceCandidate)

    console.log('ICE candidate received')

    callback = (callback || noop).bind(this)

    addIceCandidate(candidate, callback)
  }

  this.generateOffer = function (callback) {
    callback = callback.bind(this)

    newPc = createPeerConnection(configuration)

    var browser = parser.getBrowser()
    var firefox34 = (browser.name === 'Firefox' && browser.version > 34)
    var browserConstraints = firefox34 ? {
      offerToReceiveAudio: (mode !== 'sendonly'),
      offerToReceiveVideo: (mode !== 'sendonly')
    } : {
      mandatory: {
        OfferToReceiveAudio: (mode !== 'sendonly'),
        OfferToReceiveVideo: (mode !== 'sendonly')
      },
      optional: [{
        DtlsSrtpKeyAgreement: true
      }]
    }

    console.log('constraints:', constraints);

    newPc.createOffer(function (offer) {
        console.log('Created SDP offer')

        newPc.setLocalDescription(offer, function () {
            console.log('Local description set:', offer.sdp)

            callback(null, offer.sdp, self.processAnswer.bind(self))
          },
          callback)
      },
      callback, constraints)
  }

  function oniceconnectionstatechange() {
    if (this.iceConnectionState === 'connected' || this.iceConnectionState ===
      'completed') {
      this.removeEventListener('iceconnectionstatechange',
        oniceconnectionstatechange)

      if (remoteVideo) {
        var style = remoteVideo.style

        var oldBackgroundImage = style.backgroundImage;
        //        style.backgroundImage = "url('"+self.currentFrame.toDataURL()+"')"

        remoteVideo.src = URL.createObjectURL(this.getRemoteStreams()[0]);
        console.log('Remote URL: ' + remoteVideo.src)

        //        style.backgroundImage = oldBackgroundImage  // [ToDo] Restore it when new video is playing
      }

      if (pc) pc.close()
      pc = this;

      self.emit('connected')
    }
  }

  this.showLocalVideo = function () {
    localVideo.src = URL.createObjectURL(videoStream)
    localVideo.muted = true
  }

  /**
   * Callback function invoked when a SDP answer is received. Developers are
   * expected to invoke this function in order to complete the SDP negotiation.
   *
   * @function module:kurentoUtils.WebRtcPeer.prototype.processAnswer
   *
   * @param sdpAnswer - Description of sdpAnswer
   * @param callback -
   *            Invoked after the SDP answer is processed, or there is an error.
   */
  this.processAnswer = function (sdpAnswer, callback) {
    callback = (callback || noop).bind(this)

    var answer = new RTCSessionDescription({
      type: 'answer',
      sdp: sdpAnswer
    })

    console.log('SDP answer received, setting remote description')

    if (newPc.signalingState === 'closed')
      return callback('PeerConnection is closed')

    newPc.setRemoteDescription(answer, callback, callback);
  }

  /**
   * Callback function invoked when a SDP offer is received. Developers are
   * expected to invoke this function in order to complete the SDP negotiation.
   *
   * @function module:kurentoUtils.WebRtcPeer.prototype.processOffer
   *
   * @param sdpOffer - Description of sdpOffer
   * @param callback - Called when the remote description has been set
   *  successfully.
   */
  this.processOffer = function (sdpOffer, callback) {
    callback = callback.bind(this)

    var offer = new RTCSessionDescription({
      type: 'offer',
      sdp: sdpOffer
    })

    console.log('SDP offer received, setting remote description')

    newPc = createPeerConnection(configuration)

    newPc.setRemoteDescription(offer, function () {
        // Generate answer
        newPc.createAnswer(function (answer) {
            console.log('Created SDP answer')

            newPc.setLocalDescription(answer, function () {
                console.log('Local description set', answer.sdp)

                callback(null, answer.sdp);
              },
              callback)
          },
          callback)
      },
      callback)
  }

  function streamEndedListener() {
    self.emit('streamended', this)
  }

  /**
   * This method creates the RTCPeerConnection object taking into account the
   * properties received in the constructor. It starts the SDP negotiation
   * process: generates the SDP offer and invokes the onsdpoffer callback. This
   * callback is expected to send the SDP offer, in order to obtain an SDP
   * answer from another peer.
   */
  function start() {
    if (pc.signalingState === 'closed') {
      callback(
        'The peer connection object is in "closed" state. This is most likely due to an invocation of the dispose method before accepting in the dialogue'
      )
    }

    if (videoStream && localVideo) {
      self.showLocalVideo()
    }

    if (videoStream) {
      videoStream.addEventListener('ended', streamEndedListener)
      pc.addStream(videoStream)
    }

    if (audioStream) {
      audioStream.addEventListener('ended', streamEndedListener)
      pc.addStream(audioStream)
    }

    // [Hack] https://code.google.com/p/chromium/issues/detail?id=443558
    if (mode === 'sendonly') mode = 'sendrecv'

    callback()
  }

  if (mode !== 'recvonly' && !videoStream && !audioStream) {
    function getMedia(constraints) {
      constraints = Array.prototype.slice.call(arguments)
      constraints.unshift(MEDIA_CONSTRAINTS)

      getUserMedia(recursive.apply(undefined, constraints), function (stream) {
          videoStream = stream

          start()
        },
        callback)
    }

    if (sendSource === 'webcam') {
      getMedia(mediaConstraints)
    } else {
      getScreenConstraints(sendSource, function (error, constraints) {
        if (error) return callback(error)

        getMedia(constraints, mediaConstraints)
      }, guid)
    }
  } else {
    setTimeout(start, 0)
  }

  /**
   * @description This method frees the resources used by WebRtcPeer.
   *
   * @function module:kurentoUtils.WebRtcPeer.prototype.dispose
   */
  this.dispose = function () {
    console.log('Disposing WebRtcPeer');

    if (pc) {
      if (pc.signalingState === 'closed') return

      pc.getLocalStreams().forEach(streamStop)

      // FIXME This is not yet implemented in firefox
      // if(videoStream) pc.removeStream(videoStream);
      // if(audioStream) pc.removeStream(audioStream);

      pc.close();
      pc = undefined
    }

    if (localVideo) {
      localVideo.pause()
      localVideo.src = ''
      localVideo.load()
    }
    if (remoteVideo) {
      remoteVideo.pause()
      remoteVideo.src = ''
      remoteVideo.load()
    }

    self.removeAllListeners()

    if (window.cancelChooseDesktopMedia !== undefined)
      window.cancelChooseDesktopMedia(guid)
  }
}
inherits(WebRtcPeer, EventEmitter)

function trackSetEnable(track) {
  track.enabled = this.valueOf();
}

function createEnableDescriptor(type) {
  var method = 'get' + type + 'Tracks'

  return {
    enumerable: true,
    get: function () {
      // [ToDo] Should return undefined if not all tracks have the same value?

      if (!this.peerConnection) return

      var streams = this.peerConnection.getLocalStreams()
      if (!streams.length) return

      for (var i = 0, stream; stream = streams[i]; i++) {
        var tracks = stream[method]()
        for (var j = 0, track; track = tracks[j]; j++)
          if (!track.enabled) return false
      }

      return true
    },
    set: function (value) {
      this.peerConnection.getLocalStreams().forEach(function (stream) {
        stream[method]().forEach(trackSetEnable, value)
      })
    }
  }
}

Object.defineProperties(WebRtcPeer.prototype, {
  'enabled': {
    enumerable: true,
    get: function () {
      return this.audioEnabled && this.videoEnabled
    },
    set: function (value) {
      this.audioEnabled = this.videoEnabled = value
    }
  },
  'audioEnabled': createEnableDescriptor('Audio'),
  'videoEnabled': createEnableDescriptor('Video')
})

WebRtcPeer.prototype.getLocalSessionDescriptor = function () {
  var pc = this.peerConnection

  if (pc) return pc.localDescription
}

WebRtcPeer.prototype.getRemoteSessionDescriptor = function () {
  var pc = this.peerConnection

  if (pc) return pc.remoteDescription
}

WebRtcPeer.prototype.getLocalStream = function (index) {
  var pc = this.peerConnection

  if (pc) return pc.getLocalStreams()[index || 0]
}

WebRtcPeer.prototype.getRemoteStream = function (index) {
  var pc = this.peerConnection

  if (pc) return pc.getRemoteStreams()[index || 0]
}

//
// Specialized child classes
//

function WebRtcPeerRecvonly(options, callback) {
  if (!(this instanceof WebRtcPeerRecvonly)) {
    return new WebRtcPeerRecvonly(options, callback)
  }

  WebRtcPeerRecvonly.super_.call(this, 'recvonly', options, callback)
}
inherits(WebRtcPeerRecvonly, WebRtcPeer)

function WebRtcPeerSendonly(options, callback) {
  if (!(this instanceof WebRtcPeerSendonly)) {
    return new WebRtcPeerSendonly(options, callback)
  }

  WebRtcPeerSendonly.super_.call(this, 'sendonly', options, callback)
}
inherits(WebRtcPeerSendonly, WebRtcPeer)

function WebRtcPeerSendrecv(options, callback) {
  if (!(this instanceof WebRtcPeerSendrecv)) {
    return new WebRtcPeerSendrecv(options, callback)
  }

  WebRtcPeerSendrecv.super_.call(this, 'sendrecv', options, callback)
}
inherits(WebRtcPeerSendrecv, WebRtcPeer)

exports.bufferizeCandidates = bufferizeCandidates

exports.WebRtcPeerRecvonly = WebRtcPeerRecvonly
exports.WebRtcPeerSendonly = WebRtcPeerSendonly
exports.WebRtcPeerSendrecv = WebRtcPeerSendrecv
