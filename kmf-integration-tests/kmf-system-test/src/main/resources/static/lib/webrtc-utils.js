function onerror(error) {
	console.error(error);
};

function prepareSendPlayer(audio, video, audioUrl, sdpOfferReady) {

	if (audioUrl) {
		audio = false;
	}

	getUserMedia({
		'audio' : audio,
		'video' : video
	}, function(stream) {

		var videoInput = document.getElementById("local");

		videoInput.src = URL.createObjectURL(stream);

		var mediaConstraints = {
			mandatory : {
				OfferToReceiveAudio : true,
				OfferToReceiveVideo : true
			}
		};

		// Create a PeerConnection client in the browser
		var peerConnection = new RTCPeerConnection({
			iceServers : [ {
				url : 'stun:stun.l.google.com:19302'
			} ]
		}, {
			optional : [ {
				DtlsSrtpKeyAgreement : true
			} ]
		});

		if (audioUrl) {
			var context = new AudioContext();
			var audioTest = document.getElementById("audioTest");
			audioTest.src = audioUrl;
			var audioStream = context.createMediaElementSource(audioTest);
			var mixedOutput = context.createMediaStreamDestination();
			audioStream.connect(mixedOutput);
			peerConnection.addStream(mixedOutput.stream);
		}

		peerConnection.addStream(stream);

		peerConnection.createOffer(function(offer) {
			peerConnection.setLocalDescription(offer, function() {
				console.log('offer', offer.sdp);
			}, onerror);
		}, onerror, mediaConstraints);

		peerConnection.addEventListener('icecandidate', function(event) {
			if (event.candidate)
				return;

			var offer = peerConnection.localDescription;

			console.log('offer+candidates', offer.sdp);

			sdpOfferReady(peerConnection, offer);

		}, onerror);
	}, onerror);
}

function prepareReceivePlayer(peerConnection, sdpAnswer) {

	var videoOutput = document.getElementById("video");

	answer = new RTCSessionDescription({
		type : 'answer',
		sdp : sdpAnswer
	});

	console.log('answer', answer.sdp);

	peerConnection.setRemoteDescription(answer, function() {
		var stream = peerConnection.getRemoteStreams()[0];

		// Set the stream on the video tag
		videoOutput.src = URL.createObjectURL(stream);

	}, onerror);
}