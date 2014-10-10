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
 *
 */

var freeice = require('freeice');

/**
 * @description Default handler for error callbacks. The error messaged passed
 *              as argument is showed in a console, a div layer which should be
 *              previously created.
 * 
 * @function defaultOnerror
 * 
 * @param error -
 *            {String} Error message
 * 
 */
function defaultOnerror(error) {
	if (error)
		console.error(error);
}

function noop() {
};

/**
 * 
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
 * 
 */
function WebRtcPeer(mode, localVideo, remoteVideo, onsdpoffer, onerror,
		videoStream, audioStream) {

	Object.defineProperty(this, 'pc', {
		writable : true
	});

	this.localVideo = localVideo;
	this.remoteVideo = remoteVideo;
	this.onerror = onerror || defaultOnerror;
	this.stream = videoStream;
	this.audioStream = audioStream;
	this.mode = mode;
	this.onsdpoffer = onsdpoffer || noop;
}

/**
 * @description This method creates the RTCPeerConnection object taking into
 *              account the properties received in the constructor. It starts
 *              the SDP negotiation process: generates the SDP offer and invokes
 *              the onsdpoffer callback. This callback is expected to send the
 *              SDP offer, in order to obtain an SDP answer from another peer.
 * 
 * @function module:kurentoUtils.WebRtcPeer.prototype.start
 * 
 */
WebRtcPeer.prototype.start = function() {

	var self = this;

	if (!this.pc) {
		this.pc = new RTCPeerConnection(this.server, this.options);
	}

	var pc = this.pc;

	if (this.stream && this.localVideo) {
		this.localVideo.src = URL.createObjectURL(this.stream);
		this.localVideo.muted = true;
	}

	if (this.stream) {
		pc.addStream(this.stream);
	}

	if (this.audioStream) {
		pc.addStream(this.audioStream);
	}

	this.constraints = {
		mandatory : {
			OfferToReceiveAudio : (this.remoteVideo !== undefined),
			OfferToReceiveVideo : (this.remoteVideo !== undefined)
		}
	};

	pc.createOffer(function(offer) {
		console.log('Created SDP offer');
		pc.setLocalDescription(offer, function() {
			console.log('Local description set');
		}, self.onerror);

	}, this.onerror, this.constraints);

	var ended = false;
	pc.onicecandidate = function(e) {
		// candidate exists in e.candidate
		if (e.candidate) {
			ended = false;
			return;
		}

		if (ended) {
			return;
		}

		var offerSdp = pc.localDescription.sdp;
		console.log('ICE negotiation completed');

		self.onsdpoffer(offerSdp, self);
		// self.emit('sdpoffer', offerSdp);

		ended = true;
	};
}

/**
 * @description This method frees the resources used by WebRtcPeer.
 * 
 * @function module:kurentoUtils.WebRtcPeer.prototype.dispose
 * 
 */
WebRtcPeer.prototype.dispose = function() {
	console.log('Disposing WebRtcPeer');

	// FIXME This is not yet implemented in firefox
	// if (this.stream) this.pc.removeStream(this.stream);

	// For old browsers, PeerConnection.close() is NOT idempotent and raise
	// error. We check its signaling state and don't close it if it's already
	// closed
	if (this.pc && this.pc.signalingState != 'closed')
		this.pc.close();

	if (this.localVideo)
		this.localVideo.src = '';
	if (this.remoteVideo)
		this.remoteVideo.src = '';

	if (this.stream) {
		this.stream.getAudioTracks().forEach(function(track) {
			track.stop && track.stop()
		})
		this.stream.getVideoTracks().forEach(function(track) {
			track.stop && track.stop()
		})
	}
};

/**
 * @description Default user media constraints considered when invoking the
 *              getUserMedia function. These values are: maxWidth=640,
 *              maxFrameRate=15, minFrameRate=15.
 * 
 * @alias module:kurentoUtils.WebRtcPeer.prototype.userMediaConstraints
 * 
 */
WebRtcPeer.prototype.userMediaConstraints = {
	audio : true,
	video : {
		mandatory : {
			maxWidth : 640,
			maxFrameRate : 15,
			minFrameRate : 15
		}
	}
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
 * 
 */
WebRtcPeer.prototype.processSdpAnswer = function(sdpAnswer) {
	var answer = new RTCSessionDescription({
		type : 'answer',
		sdp : sdpAnswer,
	});

	console.log('SDP answer received, setting remote description');
	var self = this;
	self.pc.setRemoteDescription(answer, function() {
		if (self.remoteVideo) {
			var stream = self.pc.getRemoteStreams()[0];
			self.remoteVideo.src = URL.createObjectURL(stream);
		}
	}, this.onerror);
}

/**
 * @description Default ICE server (stun:stun.l.google.com:19302).
 * 
 * @alias module:kurentoUtils.WebRtcPeer.prototype.server
 * 
 */
WebRtcPeer.prototype.server = {
	iceServers : freeice()
};

/**
 * @description Default options (DtlsSrtpKeyAgreement=true) for
 *              RTCPeerConnection.
 * 
 * @alias module:kurentoUtils.WebRtcPeer.prototype.options
 * 
 */
WebRtcPeer.prototype.options = {
	optional : [ {
		DtlsSrtpKeyAgreement : true
	} ]
};

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
WebRtcPeer.start = function(mode, localVideo, remoteVideo, onSdp, onerror,
		mediaConstraints, videoStream, audioStream) {
	var wp = new WebRtcPeer(mode, localVideo, remoteVideo, onSdp, onerror,
			videoStream, audioStream);

	if (wp.mode !== 'recv' && !wp.stream) {
		var constraints = mediaConstraints ? mediaConstraints
				: wp.userMediaConstraints;

		getUserMedia(constraints, function(userStream) {
			wp.stream = userStream;
			wp.start();
		}, wp.onerror);
	} else {
		wp.start();
	}

	return wp;
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
		mediaConstraints) {
	return WebRtcPeer.start('recv', null, remoteVideo, onSdp, onError,
			mediaConstraints);
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
		mediaConstraints) {
	return WebRtcPeer.start('send', localVideo, null, onSdp, onError,
			mediaConstraints);
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
		mediaConstraints) {
	return WebRtcPeer.start('sendRecv', localVideo, remoteVideo, onSdp,
			onError, mediaConstraints);
};

module.exports = WebRtcPeer;
