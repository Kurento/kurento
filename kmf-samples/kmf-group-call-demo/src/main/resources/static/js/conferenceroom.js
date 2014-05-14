var client = new JsonRpcClient("ws://localhost:8080/phone/ws/websocket",
		onRequest);

function onRequest(transaction, message) {
	
	if (message.method === "newParticipantArrived") {
		onNewParticipant(transaction, message);
	} else if (message.method === "existingParticipants") {
		onExistingParticipants(transaction, message);
	} else {
		console.error("Unrecognized request: " + JSON.stringify(message));
	}
}

function onNewParticipant(transaction, request) {

	sendVideo();
	
	prepareSendPlayer(function(peerConnection, offer) {

		localPeerConnection = peerConnection;

		transaction.sendResponse({
			callResponse : "Accept",
			sdpOffer : offer.sdp
		});
	});
}

function onExistingParticipants(transaction, request) {
	prepareReceivePlayer(localPeerConnection, request.params.sdpAnswer);
	
	transaction.sendResponse({});
}

function register() {

	var name = document.getElementById("name").value;
	var room = document.getElementById("room").value;

	client.sendRequest("joinRoom", {
		name : name,
		room : room 
	}, function() {
		console.log(nae + " registered in room " + room);
	});
}

function sendVideo() {

	var peer = document.getElementById("peer").value;

	prepareSendPlayer(function(peerConnection, offer) {

		client.sendRequest("sendVideo", {
			callTo : peer,
			sdpOffer : offer.sdp
		}, function(error, result) {
			prepareReceivePlayer(peerConnection, result.sdpAnswer);
		});	
	});
}
