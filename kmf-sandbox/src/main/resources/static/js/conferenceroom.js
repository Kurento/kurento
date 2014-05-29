var client = new JsonRpcClient("ws://192.168.0.150:8080/groupcall/ws/websocket",
		onRequest);

function onRequest(transaction, message) {

	//TODO no message defined for now. This block will always go through the else clause
	if (message.method === "existingParticipants") {
		onExistingParticipants(transaction, message);
	} else {
		console.error("Unrecognized request: " + JSON.stringify(message));
	}
}

function onRequest(message) {

	if (message.method === "newParticipantArrived") {
		onNewParticipant(message);
	} else if (message.method === "participantLeft") {
		onParticipantLeft(message);
	} else {
		console.error("Unrecognized request: " + JSON.stringify(message));
	}
}


function register() {

	var name = document.getElementById("name").value;
	var room = document.getElementById("roomName").value;

	document.getElementById('join').style.display = 'none';
	document.getElementById('room').style.display = 'block';

	client.sendRequest("joinRoom", {
		name : name,
		room : room,
	}, function(error, result) {
		console.log(name + " registered in room " + room);
		var participant = new Participant(name);
		document.getElementById('room').appendChild(participant.getElement());
		prepareSendPreviewPlayer(participant.getVideoElement(), participant.offerToReceiveVideo.bind(participant));
		result.value.forEach(receiveVideo);
	});
}

function onNewParticipant(request) {
	receiveVideo(request.params.name);
}

function receiveVideo(sender) {
	var participant = new Participant(sender);
	document.getElementById('room').appendChild(participant.getElement());
	var video = participant.getVideoElement();
	prepareReceiveOnlyPlayer(video, participant.offerToReceiveVideo.bind(participant));
}

function onParticipantLeft(request) {
	console.log('Participant ' + request.params.name + ' left');
	document.removeChild(document.getElementById(request.params.name));
}

