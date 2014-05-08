var client = new JsonRpcClient("ws://localhost:8080/phone/ws/websocket",
		onRequest);

var localPeerConnection;

function onRequest(transaction, message) {
	
	if (message.method === "incommingCall") {
		onIncommingCall(transaction, message);
	} else if (message.method === "startCommunication") {
		onStartCommunication(transaction, message);
	} else {
		console.error("Unrecognized request: " + JSON.stringify(message));
	}
}

function onIncommingCall(transaction, request) {

	prepareSendPlayer(function(peerConnection, offer) {

		localPeerConnection = peerConnection;

		transaction.sendResponse({
			callResponse : "Accept",
			sdpOffer : offer.sdp
		});
	});
}

function onStartCommunication(transaction, request) {
	prepareReceivePlayer(localPeerConnection, request.params.sdpAnswer);
	
	transaction.sendResponse({});
}

function register() {

	var name = document.getElementById("name").value;

	client.sendRequest("register", {
		name : name
	}, function() {
		console.log("registered");
	});
}

function call() {

	var peer = document.getElementById("peer").value;

	prepareSendPlayer(function(peerConnection, offer) {

		localPeerConnection = peerConnection;
		
		client.sendRequest("call", {
			callTo : peer,
			sdpOffer : offer.sdp
		}, function(error, result) {
			prepareReceivePlayer(localPeerConnection, result.sdpAnswer);			
		});	
	});
}