function defaultOnerror(error) {
	console.error(error);
}

function WebRtcPeer() {

	var onerror = defaultOnerror;
	var onSdpOffer;
	var stream;

	var server = {
			iceServers : [ {
				url : 'stun:stun.l.google.com:19302'
			} ]
	};

	var options = {
			optional : [ {
				DtlsSrtpKeyAgreement : true
			} ]
	};

	var pc = new RTCPeerConnection(server, options);

	Object.defineProperty(this, 'onsdpoffer', { 
		get:function() { return onSdpOffer; },
		set:function(value) { onSdpOffer = value; }
	});

	Object.defineProperty(this, 'onerror', { 
		get:function() { return onerror; },
		set:function(value) { onerror = value; }
	});

	Object.defineProperty(this, 'stream',  { 
		get:function() { return stream; },
		set:function(value) { stream = value; }
	});

	Object.defineProperty(this, 'connection', {
		get:function() { return pc; }
	});

	this.dispose = function() {
		console.log('Disposing WebRtcPeer');
		//TODO don't know if we have to do this
		if (stream) pc.removeStream(stream);
		pc.close();
	};
}

WebRtcPeer.start = function(localVideo, remoteVideo, onSdp, onerror, videoStream) {
	var wp = new WebRtcPeer();

	wp.onsdpoffer = onSdp;
	wp.onerror = onerror ? onerror : defaultOnerror;
	var constraints = {
			mandatory: {
				OfferToReceiveAudio: (remoteVideo !== undefined),
				OfferToReceiveVideo: (remoteVideo !== undefined)
			}
	};

	var pc = wp.connection;

	if (localVideo) {
		localVideo.src = URL.createObjectURL(videoStream);
		localVideo.muted = true;
	}

	if (videoStream) {
		pc.addStream(videoStream);
	}

	pc.createOffer(function(offer) {
		console.log('Created SDP offer');
		pc.setLocalDescription(offer, function() {
			console.log('Local description set');
		}, onerror);

	}, onerror, constraints);

	function processSdpAnswer(sdpAnswer) {
		var answer = new RTCSessionDescription({
			type : 'answer',
			sdp : sdpAnswer,
		});

		console.log('SDP answer received, setting remote description');

		pc.setRemoteDescription(answer, function() {
			if (remoteVideo) {
				var stream = pc.getRemoteStreams()[0];
				remoteVideo.src = URL.createObjectURL(stream);
			}
		}, onerror);
	}

	pc.onicecandidate = function (e) {
		// candidate exists in e.candidate
		if (e.candidate) return;

		var offerSdp =  pc.localDescription.sdp;
		console.log('ICE negotiation completed');
		if (wp.onsdpoffer) {
			console.log('Invokin SDP offer callback function');
			wp.onsdpoffer(offerSdp, processSdpAnswer);	
		}
	};

	return wp;
};

WebRtcPeer.startRecvOnly = function (remoteVideo, onSdp, onError) {
	WebRtcPeer.start(null, remoteVideo, onSdp, onError);
};

WebRtcPeer.startSendOnly = function (localVideo, onSdp, onError) {
	if (!onError) onError = defaultOnerror;

	getUserMedia({
		'audio' : true,
		'video' : true
	}, function(userStream) {
		WebRtcPeer.start(localVideo, null, onSdp, onError, userStream);
	}, onError);
};

WebRtcPeer.startSendRecv = function (localVideo, remoteVideo, onSdp, onError) {
	if (!onError) onError = defaultOnerror;

	getUserMedia({
		'audio' : true,
		'video' : true
	}, function(userStream) {
		WebRtcPeer.start(localVideo, remoteVideo, onSdp, onError, userStream);
	}, onError);
};

module.exports = WebRtcPeer;