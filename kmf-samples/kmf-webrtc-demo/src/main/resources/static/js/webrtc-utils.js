function onerror(error) {
	console.error(error);
};

function prepareSendPlayer(sdpOfferReady) {

	getUserMedia({
		'audio' : true,
		'video' : true
	}, function(stream) {

		var videoInput = document.getElementById("videoInput");

		videoInput.src = URL.createObjectURL(stream);

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

		peerConnection.addStream(stream);

		peerConnection.createOffer(function(offer) {
			peerConnection.setLocalDescription(offer, function() {
				console.log('offer', offer.sdp);
			}, onerror);
		}, onerror);

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

	var videoOutput = document.getElementById("videoOutput");

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