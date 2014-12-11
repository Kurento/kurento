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

function KurentoRoomClient(options, callback) {

	if (!(this instanceof KurentoRoomClient))
	    return new KurentoRoomClient(options, callback);

	var wsUri = options.wsUri;
	var participants = {};
	var name;

	var ws = new WebSocket(wsUri);

	var that = this;

	ws.onmessage = function(message) {

		var parsedMessage = JSON.parse(message.data);
		console.info('Received message: ' + message.data);

		switch (parsedMessage.id) {
		case 'existingParticipants':
			onExistingParticipants(parsedMessage);
			break;
		case 'newParticipantArrived':
			onNewParticipant(parsedMessage);
			break;
		case 'participantLeft':
			onParticipantLeft(parsedMessage);
			break;
		case 'receiveVideoAnswer':
			receiveVideoResponse(parsedMessage);
			break;
		default:
			console.error('Unrecognized message', parsedMessage);
		}
	}

	ws.onopen = function(){
	    callback(null, that);
	}

	ws.onerror = function(evt){
	    callback(evt.data);
	}

	ws.onclose = function(){
	    console.log("Connection Closed");
	}

	this.joinRoom = function(participantName, roomName) {

		name = participantName;

		that.sendMessage({
			id : 'joinRoom',
			name : participantName,
			room : roomName,
		});
	}

	this.leaveRoom = function() {

		that.sendMessage({
			id : 'leaveRoom'
		});

		for (var key in participants) {
			participants[key].dispose();
		}

		document.getElementById('join').style.display = 'block';
		document.getElementById('room').style.display = 'none';

		ws.close();
	}

	this.sendMessage = function(message) {
		var jsonMessage = JSON.stringify(message);
		console.log('Sending message: ' + jsonMessage);
		ws.send(jsonMessage);
		console.log('Sent message: ' + jsonMessage);
	}

	function onNewParticipant(request) {
		receiveVideo(request.name);
	}

	function receiveVideoResponse(result) {
		var participant = participants[result.name];
		if(participant !== undefined){
			participant.rtcPeer.processSdpAnswer(result.sdpAnswer);
		} else {
			console.warn("Receiving video response from an unexisting user");
		}
	}

	function onExistingParticipants(msg) {

		//Enable and disable iceServers from code
		kurentoUtils.WebRtcPeer.prototype.server.iceServers = [];

		var constraints = {
			audio : true,
			video : {
				mandatory : {
					maxWidth : 320,
					maxFrameRate : 15,
					minFrameRate : 15
				}
			}
		};

		console.log(name + " registered in room " + room);

		var participant = new RoomParticipant(name, options.participantsId, that);
		participants[name] = participant;

		var video = participant.getVideoElement();
		console.log(name + " before webrtcutils");

		participant.rtcPeer = kurentoUtils.WebRtcPeer.startSendOnly(video,
				participant.offerToReceiveVideo.bind(participant), null,
				constraints);

		console.log(name + " waiting for SDP offer");
		msg.data.forEach(receiveVideo);
	}

	function receiveVideo(sender) {
		var participant = new RoomParticipant(sender, options.participantsId, that);
		participants[sender] = participant;

		var video = participant.getVideoElement();
		participant.rtcPeer = kurentoUtils.WebRtcPeer.startRecvOnly(video,
				participant.offerToReceiveVideo.bind(participant));
	}

	function onParticipantLeft(request) {
		console.log('Participant ' + request.name + ' left');
		var participant = participants[request.name];
		participant.dispose();
		delete participants[request.name];
	}

	this.close = function(){
		if(name !== undefined){
			leaveRoom();
		}
	}
}




