function defaultOnerror(error) {
	console.error(error);
}

/**
 * 
 * @param {send | receive | send_recv | send_prev} mode
 * @param video
 * @param stream
 * @param sendOfferSdpCallback
 * @param onerror
 */
function WebRtcPlayer(mode, video, stream, sendOfferSdpCallback, onerror) {

	if (!onerror) {
		onerror = defaultOnerror;
	}

	var send = false;
	var recv = false;
	var preview = 'none';

	switch(mode) {
	case 'send' : 
		send = true;
		break;
	case 'recv' : 
		send = false;
		recv = true;
		preview = 'remote';
		break;
	case 'send_recv' : 
		send =  true;
		recv = true;
		preview = 'remote';
		break;
	case 'send_prev' : 
		send = true;
		preview = 'local';
		break;
	default:
		alert('Unknown mode ' + mode);
	}

	var constraints = {
			mandatory: {
				OfferToReceiveAudio: recv,
				OfferToReceiveVideo: recv
			}
	};

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

	if (preview === 'local') {
		video.src = URL.createObjectURL(stream);
	}

	if (send) {
		pc.addStream(stream);
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
			sdp : sdpAnswer
		});

		console.log('SDP answer received, setting remote description');

		pc.setRemoteDescription(answer, function() {

			if (recv) {
				stream = pc.getRemoteStreams()[0];
			}

			// Set the stream on the video tag
			video.src = URL.createObjectURL(stream);

		}, onerror);
	}

	pc.onicecandidate = function (e) {
		// candidate exists in e.candidate
		if (e.candidate) return;

		var offerSdp =  pc.localDescription.sdp;
		console.log('ICE negotiation completed');
		if (sendOfferSdpCallback) {
			sendOfferSdpCallback(offerSdp, processSdpAnswer);	
		}
	};

}

function prepareReceiveOnlyPlayer(video, sendOfferSdpCallback) {
	WebRtcPlayer('recv', video, null, sendOfferSdpCallback);
}

function prepareSendOnlyPlayer(video, sendOfferSdpCallback) {
	getUserMedia({
		'audio' : true,
		'video' : true
	}, function(stream) {
		WebRtcPlayer('send', video, stream, sendOfferSdpCallback);
		video.muted = true;
	}, defaultOnerror);
}

function prepareSendPreviewPlayer(video, sendOfferSdpCallback) {
	getUserMedia({
		'audio' : true,
		'video' : true
	}, function(stream) {
		WebRtcPlayer('send_prev', video, stream, sendOfferSdpCallback);
		video.muted = true;
	}, defaultOnerror);
}

function prepareSendAndReceivePlayer(video, sendOfferSdpCallback, videoLoopback) {
	getUserMedia({
		'audio' : true,
		'video' : true
	}, function(stream) {
		WebRtcPlayer('send_recv', video, stream, sendOfferSdpCallback);
		videoLoopback.src = URL.createObjectURL(stream);
		videoLoopback.muted = true;
	}, defaultOnerror);
}
