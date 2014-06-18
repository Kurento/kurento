var client = new RpcBuilder.clients.JsonRpcClient('ws://' + location.host + '/groupcall/ws/websocket',
		onRequest);

var participants = {};

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

	document.getElementById('room-header').innerText = 'ROOM ' + room;
	document.getElementById('join').style.display = 'none';
	document.getElementById('room').style.display = 'block';

	client.sendRequest("joinRoom", {
		name : name,
		room : room,
	}, function(error, result) {
		console.log(name + " registered in room " + room);
		var participant = new Participant(name);
		participants[name] = participant;
		participant.rtcPeer = kwsUtils.WebRtcPeer.startSendOnly(participant.getVideoElement(), participant.offerToReceiveVideo.bind(participant));
		result.value.forEach(receiveVideo);
	});
}

function onNewParticipant(request) {
	receiveVideo(request.params.name);
}

function leaveRoom() {
	client.sendRequest('leaveRoom', {
	} , function(error, result) {
	});
	
	for (var key in participants) {
		participants[key].dispose();
	}
	
	document.getElementById('join').style.display = 'block';
	document.getElementById('room').style.display = 'none';
}

function receiveVideo(sender) {
	var participant = new Participant(sender);
	participants[sender] = participant;
	var video = participant.getVideoElement();
	participant.rtcPeer = kwsUtils.WebRtcPeer.startRecvOnly(video, participant.offerToReceiveVideo.bind(participant));
}

function onParticipantLeft(request) {
	console.log('Participant ' + request.params.name + ' left');
	var participant = participants[request.params.name];
	participant.dispose();
	delete participants[request.params.name];
}
