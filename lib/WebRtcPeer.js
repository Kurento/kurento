/*
 * (C) Copyright 2014-2015 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var freeice = require('freeice')
var inherits = require('inherits')
var UAParser = require('ua-parser-js')
var uuidv4 = require('uuid/v4')
var hark = require('hark')

var EventEmitter = require('events').EventEmitter
var recursive = require('merge').recursive.bind(undefined, true)
var sdpTranslator = require('sdp-translator')
var logger = (typeof window === 'undefined') ? console : window.Logger ||
  console

try {
  require('kurento-browser-extensions')
} catch (error) {
  if (typeof getScreenConstraints === 'undefined') {
    logger.warn('screen sharing is not available')

    getScreenConstraints = function getScreenConstraints(sendSource, callback) {
      callback(new Error("This library is not enabled for screen sharing"))
    }
  }
}

var MEDIA_CONSTRAINTS = {
  audio: true,
  video: {
    width: 640,
    framerate: 15
  }
}

// Somehow, the UAParser constructor gets an empty window object.
// We need to pass the user agent string in order to get information
var ua = (typeof window !== 'undefined' && window.navigator) ? window.navigator
  .userAgent : ''
var parser = new UAParser(ua)
var browser = parser.getBrowser()

function insertScriptSrcInHtmlDom(scriptSrc) {
  //Create a script tag
  var script = document.createElement('script');
  // Assign a URL to the script element
  script.src = scriptSrc;
  // Get the first script tag on the page (we'll insert our new one before it)
  var ref = document.querySelector('script');
  // Insert the new node before the reference node
  ref.parentNode.insertBefore(script, ref);
}

function importScriptsDependsOnBrowser() {
  if (browser.name === 'IE') {
    insertScriptSrcInHtmlDom(
      "https://cdn.temasys.io/adapterjs/0.15.x/adapter.debug.js");
  }
}

importScriptsDependsOnBrowser();
var usePlanB = false
if (browser.name === 'Chrome' || browser.name === 'Chromium') {
  logger.debug(browser.name + ": using SDP PlanB")
  usePlanB = true
}

function noop(error) {
  if (error) logger.error(error)
}

function trackStop(track) {
  track.stop && track.stop()
}

function streamStop(stream) {
  stream.getTracks().forEach(trackStop)
}

/**
 * Returns a string representation of a SessionDescription object.
 */
var dumpSDP = function (description) {
  if (typeof description === 'undefined' || description === null) {
    return ''
  }

  return 'type: ' + description.type + '\r\n' + description.sdp
}

function bufferizeCandidates(pc, onerror) {
  var candidatesQueue = []

  function setSignalingstatechangeAccordingWwebBrowser(functionToExecute, pc) {
    if (typeof AdapterJS !== 'undefined' && AdapterJS.webrtcDetectedBrowser ===
      'IE' && AdapterJS.webrtcDetectedVersion >= 9) {
      pc.onsignalingstatechange = functionToExecute;
    } else {
      pc.addEventListener('signalingstatechange', functionToExecute);
    }

  }

  var signalingstatechangeFunction = function () {
    if (pc.signalingState === 'stable') {
      while (candidatesQueue.length) {
        var entry = candidatesQueue.shift();
        pc.addIceCandidate(entry.candidate, entry.callback, entry.callback);
      }
    }
  };

  setSignalingstatechangeAccordingWwebBrowser(signalingstatechangeFunction, pc);
  return function (candidate, callback) {
    callback = callback || onerror;
    switch (pc.signalingState) {
    case 'closed':
      callback(new Error('PeerConnection object is closed'));
      break;
    case 'stable':
      if (pc.remoteDescription) {
        pc.addIceCandidate(candidate, callback, callback);
        break;

      }
      default:
        candidatesQueue.push({
          candidate: candidate,
          callback: callback

        });
    }
  };
}

/* Simulcast utilities */

function removeFIDFromOffer(sdp) {
  var n = sdp.indexOf("a=ssrc-group:FID");

  if (n > 0) {
    return sdp.slice(0, n);
  } else {
    return sdp;
  }
}

function getSimulcastInfo(videoStream) {
  var videoTracks = videoStream.getVideoTracks();
  if (!videoTracks.length) {
    logger.warn('No video tracks available in the video stream')
    return ''
  }
  var lines = [
    'a=x-google-flag:conference',
    'a=ssrc-group:SIM 1 2 3',
    'a=ssrc:1 cname:localVideo',
    'a=ssrc:1 msid:' + videoStream.id + ' ' + videoTracks[0].id,
    'a=ssrc:1 mslabel:' + videoStream.id,
    'a=ssrc:1 label:' + videoTracks[0].id,
    'a=ssrc:2 cname:localVideo',
    'a=ssrc:2 msid:' + videoStream.id + ' ' + videoTracks[0].id,
    'a=ssrc:2 mslabel:' + videoStream.id,
    'a=ssrc:2 label:' + videoTracks[0].id,
    'a=ssrc:3 cname:localVideo',
    'a=ssrc:3 msid:' + videoStream.id + ' ' + videoTracks[0].id,
    'a=ssrc:3 mslabel:' + videoStream.id,
    'a=ssrc:3 label:' + videoTracks[0].id
  ];

  lines.push('');

  return lines.join('\n');
}

function sleep(milliseconds) {
  var start = new Date().getTime();
  for (var i = 0; i < 1e7; i++) {
    if ((new Date().getTime() - start) > milliseconds) {
      break;
    }
  }
}

function setIceCandidateAccordingWebBrowser(functionToExecute, pc) {
  if (typeof AdapterJS !== 'undefined' && AdapterJS.webrtcDetectedBrowser ===
    'IE' && AdapterJS.webrtcDetectedVersion >= 9) {
    pc.onicecandidate = functionToExecute;
  } else {
    pc.addEventListener('icecandidate', functionToExecute);
  }

}

/**
 * Wrapper object of an RTCPeerConnection. This object is aimed to simplify the
 * development of WebRTC-based applications.
 *
 * @constructor module:kurentoUtils.WebRtcPeer
 *
 * @param {String} mode Mode in which the PeerConnection will be configured.
 *  Valid values are: 'recvonly', 'sendonly', and 'sendrecv'
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

  var self = this
  var localVideo = options.localVideo
  var remoteVideo = options.remoteVideo
  var videoStream = options.videoStream
  var audioStream = options.audioStream
  var mediaConstraints = options.mediaConstraints

  var pc = options.peerConnection
  var sendSource = options.sendSource || 'webcam'

  var dataChannelConfig = options.dataChannelConfig
  var useDataChannels = options.dataChannels || false
  var dataChannel

  var guid = uuidv4()
  var configuration = recursive({
      iceServers: freeice()
    },
    options.configuration)

  var onicecandidate = options.onicecandidate
  if (onicecandidate) this.on('icecandidate', onicecandidate)

  var oncandidategatheringdone = options.oncandidategatheringdone
  if (oncandidategatheringdone) {
    this.on('candidategatheringdone', oncandidategatheringdone)
  }

  var simulcast = options.simulcast
  var multistream = options.multistream
  var interop = new sdpTranslator.Interop()
  var candidatesQueueOut = []
  var candidategatheringdone = false

  Object.defineProperties(this, {
    'peerConnection': {
      get: function () {
        return pc
      }
    },

    'id': {
      value: options.id || guid,
      writable: false
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

    'dataChannel': {
      get: function () {
        return dataChannel
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

  // Init PeerConnection
  if (!pc) {
    pc = new RTCPeerConnection(configuration);
    if (useDataChannels && !dataChannel) {
      var dcId = 'WebRtcPeer-' + self.id
      var dcOptions = undefined
      if (dataChannelConfig) {
        dcId = dataChannelConfig.id || dcId
        dcOptions = dataChannelConfig.options
      }
      dataChannel = pc.createDataChannel(dcId, dcOptions);
      if (dataChannelConfig) {
        dataChannel.onopen = dataChannelConfig.onopen;
        dataChannel.onclose = dataChannelConfig.onclose;
        dataChannel.onmessage = dataChannelConfig.onmessage;
        dataChannel.onbufferedamountlow = dataChannelConfig.onbufferedamountlow;
        dataChannel.onerror = dataChannelConfig.onerror || noop;
      }
    }
  }

  // Shims over the now deprecated getLocalStreams() and getRemoteStreams()
  // (usage of these methods should be dropped altogether)
  if (!pc.getLocalStreams && pc.getSenders) {
    pc.getLocalStreams = function () {
      var stream = new MediaStream();
      pc.getSenders().forEach(function (sender) {
        stream.addTrack(sender.track);
      });
      return [stream];
    };
  }
  if (!pc.getRemoteStreams && pc.getReceivers) {
    pc.getRemoteStreams = function () {
      var stream = new MediaStream();
      pc.getReceivers().forEach(function (sender) {
        stream.addTrack(sender.track);
      });
      return [stream];
    };
  }

  // If event.candidate == null, it means that candidate gathering has finished
  // and RTCPeerConnection.iceGatheringState == "complete".
  // Such candidate does not need to be sent to the remote peer.
  // https://developer.mozilla.org/en-US/docs/Web/API/RTCPeerConnection/icecandidate_event#Indicating_that_ICE_gathering_is_complete
  var iceCandidateFunction = function (event) {
    var candidate = event.candidate;
    if (EventEmitter.listenerCount(self, 'icecandidate') || EventEmitter
      .listenerCount(self, 'candidategatheringdone')) {
      if (candidate) {
        var cand;
        if (multistream && usePlanB) {
          cand = interop.candidateToUnifiedPlan(candidate);
        } else {
          cand = candidate;
        }
        if (typeof AdapterJS === 'undefined') {
          self.emit('icecandidate', cand);

        }
        candidategatheringdone = false;
      } else if (!candidategatheringdone) {
        if (typeof AdapterJS !== 'undefined' && AdapterJS
          .webrtcDetectedBrowser === 'IE' && AdapterJS
          .webrtcDetectedVersion >= 9) {
          EventEmitter.prototype.emit('candidategatheringdone', cand);
        } else {
          self.emit('candidategatheringdone');
        }
        candidategatheringdone = true;
      }
    } else if (!candidategatheringdone) {
      candidatesQueueOut.push(candidate);
      if (!candidate)
        candidategatheringdone = true;

    }
  };

  setIceCandidateAccordingWebBrowser(iceCandidateFunction, pc);
  pc.onaddstream = options.onaddstream
  pc.onnegotiationneeded = options.onnegotiationneeded
  this.on('newListener', function (event, listener) {
    if (event === 'icecandidate' || event === 'candidategatheringdone') {
      while (candidatesQueueOut.length) {
        var candidate = candidatesQueueOut.shift()

        if (!candidate === (event === 'candidategatheringdone')) {
          listener(candidate)
        }
      }
    }
  })

  var addIceCandidate = bufferizeCandidates(pc)

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
    var candidate

    if (multistream && usePlanB) {
      candidate = interop.candidateToPlanB(iceCandidate)
    } else {
      candidate = new RTCIceCandidate(iceCandidate)
    }

    logger.debug('Remote ICE candidate received', iceCandidate)
    callback = (callback || noop).bind(this)
    addIceCandidate(candidate, callback)
  }

  this.generateOffer = function (callback) {
    callback = callback.bind(this)

    if (mode === 'recvonly') {
      /* Add reception tracks on the RTCPeerConnection. Send tracks are
       * unconditionally added to "sendonly" and "sendrecv" modes, in the
       * constructor's "start()" method, but nothing is done for "recvonly".
       *
       * Here, we add new transceivers to receive audio and/or video, so the
       * SDP Offer that will be generated by the PC includes these medias
       * with the "a=recvonly" attribute.
       */
      var useAudio =
        (mediaConstraints && typeof mediaConstraints.audio === 'boolean') ?
        mediaConstraints.audio : true
      var useVideo =
        (mediaConstraints && typeof mediaConstraints.video === 'boolean') ?
        mediaConstraints.video : true

      if (useAudio) {
        pc.addTransceiver('audio', {
          direction: 'recvonly'
        });
      }

      if (useVideo) {
        pc.addTransceiver('video', {
          direction: 'recvonly'
        });
      }
    } else if (mode === 'sendonly') {
      /* The constructor's "start()" method already added any available track,
       * which by default creates Transceiver with "sendrecv" direction.
       *
       * Here, we set all transceivers to only send audio and/or video, so the
       * SDP Offer that will be generated by the PC includes these medias
       * with the "a=sendonly" attribute.
       */
      pc.getTransceivers().forEach(function (transceiver) {
        transceiver.direction = "sendonly";
      });
    }

    if (typeof AdapterJS !== 'undefined' && AdapterJS
      .webrtcDetectedBrowser === 'IE' && AdapterJS.webrtcDetectedVersion >= 9
    ) {
      var setLocalDescriptionOnSuccess = function () {
        sleep(1000);
        var localDescription = pc.localDescription;
        logger.debug('Local description set\n', localDescription.sdp);
        if (multistream && usePlanB) {
          localDescription = interop.toUnifiedPlan(localDescription);
          logger.debug('offer::origPlanB->UnifiedPlan', dumpSDP(
            localDescription));
        }
        callback(null, localDescription.sdp, self.processAnswer.bind(self));
      };
      var createOfferOnSuccess = function (offer) {
        logger.debug('Created SDP offer');
        logger.debug('Local description set\n', pc.localDescription);
        pc.setLocalDescription(offer, setLocalDescriptionOnSuccess,
          callback);
      };
      pc.createOffer(createOfferOnSuccess, callback);
    } else {
      pc.createOffer()
        .then(function (offer) {
          logger.debug('Created SDP offer');
          offer = mangleSdpToAddSimulcast(offer);
          return pc.setLocalDescription(offer);
        })
        .then(function () {
          var localDescription = pc.localDescription;
          logger.debug('Local description set\n', localDescription.sdp);
          if (multistream && usePlanB) {
            localDescription = interop.toUnifiedPlan(localDescription);
            logger.debug('offer::origPlanB->UnifiedPlan', dumpSDP(
              localDescription));
          }
          callback(null, localDescription.sdp, self.processAnswer.bind(
            self));
        })
        .catch(callback);
    }
  }

  this.getLocalSessionDescriptor = function () {
    return pc.localDescription
  }

  this.getRemoteSessionDescriptor = function () {
    return pc.remoteDescription
  }

  function setRemoteVideo() {
    if (remoteVideo) {
      remoteVideo.pause()

      var stream = pc.getRemoteStreams()[0]
      remoteVideo.srcObject = stream
      logger.debug('Remote stream:', stream)

      if (typeof AdapterJS !== 'undefined' && AdapterJS
        .webrtcDetectedBrowser === 'IE' && AdapterJS.webrtcDetectedVersion >= 9
      ) {
        remoteVideo = attachMediaStream(remoteVideo, stream);
      } else {
        remoteVideo.load();
      }
    }
  }

  this.showLocalVideo = function () {
    localVideo.srcObject = videoStream
    localVideo.muted = true

    if (typeof AdapterJS !== 'undefined' && AdapterJS
      .webrtcDetectedBrowser === 'IE' && AdapterJS.webrtcDetectedVersion >= 9
    ) {
      localVideo = attachMediaStream(localVideo, videoStream);
    }
  };
  this.send = function (data) {
    if (dataChannel && dataChannel.readyState === 'open') {
      dataChannel.send(data)
    } else {
      logger.warn(
        'Trying to send data over a non-existing or closed data channel')
    }
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

    if (multistream && usePlanB) {
      var planBAnswer = interop.toPlanB(answer)
      logger.debug('asnwer::planB', dumpSDP(planBAnswer))
      answer = planBAnswer
    }

    logger.debug('SDP answer received, setting remote description')

    if (pc.signalingState === 'closed') {
      return callback('PeerConnection is closed')
    }

    pc.setRemoteDescription(answer).then(function () {
        setRemoteVideo()

        callback()
      },
      callback)
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

    if (multistream && usePlanB) {
      var planBOffer = interop.toPlanB(offer)
      logger.debug('offer::planB', dumpSDP(planBOffer))
      offer = planBOffer
    }

    logger.debug('SDP offer received, setting remote description')

    if (pc.signalingState === 'closed') {
      return callback('PeerConnection is closed')
    }

    pc.setRemoteDescription(offer).then(function () {
      return setRemoteVideo()
    }).then(function () {
      return pc.createAnswer()
    }).then(function (answer) {
      answer = mangleSdpToAddSimulcast(answer)
      logger.debug('Created SDP answer')
      return pc.setLocalDescription(answer)
    }).then(function () {
      var localDescription = pc.localDescription
      if (multistream && usePlanB) {
        localDescription = interop.toUnifiedPlan(localDescription)
        logger.debug('answer::origPlanB->UnifiedPlan', dumpSDP(
          localDescription))
      }
      logger.debug('Local description set\n', localDescription.sdp)
      callback(null, localDescription.sdp)
    }).catch(callback)
  }

  function mangleSdpToAddSimulcast(answer) {
    if (simulcast) {
      if (browser.name === 'Chrome' || browser.name === 'Chromium') {
        logger.debug('Adding multicast info')
        answer = new RTCSessionDescription({
          'type': answer.type,
          'sdp': removeFIDFromOffer(answer.sdp) + getSimulcastInfo(
            videoStream)
        })
      } else {
        logger.warn('Simulcast is only available in Chrome browser.')
      }
    }

    return answer
  }

  /**
   * This function creates the RTCPeerConnection object taking into account the
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
      videoStream.getTracks().forEach(function (track) {
        pc.addTrack(track, videoStream);
      });
    }

    if (audioStream) {
      audioStream.getTracks().forEach(function (track) {
        pc.addTrack(track, audioStream);
      });
    }

    callback()
  }

  if (mode !== 'recvonly' && !videoStream && !audioStream) {
    function getMedia(constraints) {
      if (constraints === undefined) {
        constraints = MEDIA_CONSTRAINTS
      }
      if (typeof AdapterJS !== 'undefined' && AdapterJS
        .webrtcDetectedBrowser === 'IE' && AdapterJS.webrtcDetectedVersion >= 9
      ) {
        navigator.getUserMedia(constraints, function (stream) {
          videoStream = stream;
          start();
        }, callback);
      } else {
        navigator.mediaDevices.getUserMedia(constraints).then(function (
          stream) {
          videoStream = stream;

          start();
        }).catch(callback);
      }
    }
    if (sendSource === 'webcam') {
      getMedia(mediaConstraints)
    } else {
      getScreenConstraints(sendSource, function (error, constraints_) {
        if (error)
          return callback(error)

        constraints = [mediaConstraints]
        constraints.unshift(constraints_)
        getMedia(recursive.apply(undefined, constraints))
      }, guid)
    }
  } else {
    setTimeout(start, 0)
  }

  this.on('_dispose', function () {
    if (localVideo) {
      localVideo.pause();
      localVideo.srcObject = null;

      if (typeof AdapterJS === 'undefined') {
        localVideo.load();
      }
      localVideo.muted = false;

    }
    if (remoteVideo) {
      remoteVideo.pause();
      remoteVideo.srcObject = null;
      if (typeof AdapterJS === 'undefined') {
        remoteVideo.load();

      }
    }
    self.removeAllListeners();

    if (typeof window !== 'undefined' && window.cancelChooseDesktopMedia !==
      undefined) {
      window.cancelChooseDesktopMedia(guid)
    }
  })
}
inherits(WebRtcPeer, EventEmitter)

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
      function trackSetEnable(track) {
        track.enabled = value
      }

      this.peerConnection.getLocalStreams().forEach(function (stream) {
        stream[method]().forEach(trackSetEnable)
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

WebRtcPeer.prototype.getLocalStream = function (index) {
  if (this.peerConnection) {
    return this.peerConnection.getLocalStreams()[index || 0]
  }
}

WebRtcPeer.prototype.getRemoteStream = function (index) {
  if (this.peerConnection) {
    return this.peerConnection.getRemoteStreams()[index || 0]
  }
}

/**
 * @description This method frees the resources used by WebRtcPeer.
 *
 * @function module:kurentoUtils.WebRtcPeer.prototype.dispose
 */
WebRtcPeer.prototype.dispose = function () {
  logger.debug('Disposing WebRtcPeer')

  var pc = this.peerConnection
  var dc = this.dataChannel
  try {
    if (dc) {
      if (dc.readyState === 'closed') return

      dc.close()
    }

    if (pc) {
      if (pc.signalingState === 'closed') return

      pc.getLocalStreams().forEach(streamStop)

      // FIXME This is not yet implemented in firefox
      // if(videoStream) pc.removeStream(videoStream);
      // if(audioStream) pc.removeStream(audioStream);

      pc.close()
    }
  } catch (err) {
    logger.warn('Exception disposing webrtc peer ' + err)
  }

  if (typeof AdapterJS === 'undefined') {
    this.emit('_dispose');
  }

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

function harkUtils(stream, options) {
  return hark(stream, options);
}

exports.bufferizeCandidates = bufferizeCandidates

exports.WebRtcPeerRecvonly = WebRtcPeerRecvonly
exports.WebRtcPeerSendonly = WebRtcPeerSendonly
exports.WebRtcPeerSendrecv = WebRtcPeerSendrecv
exports.hark = harkUtils
