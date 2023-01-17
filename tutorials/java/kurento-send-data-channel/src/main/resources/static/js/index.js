/*
 * (C) Copyright 2014-2016 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

var ws = new WebSocket('wss://' + location.host + '/senddatachannel');
var videoOutput;
var webRtcPeer;
var state = null;
var channel;

const I_CAN_START = 0;
const I_CAN_STOP = 1;
const I_AM_STARTING = 2;

var chanId = 0;

function getChannelName() {
	return "TestChannel" + chanId++;
}

window.onload = function() {
	console = new Console();
	console.log("Page loaded ...");
	videoOutput = document.getElementById('videoOutput');
	setState(I_CAN_START);
}

window.onbeforeunload = function() {
	ws.close();
}

ws.onmessage = function(message) {
	var parsedMessage = JSON.parse(message.data);
	console.info('Received message: ' + message.data);

	switch (parsedMessage.id) {
	case 'startResponse':
		startResponse(parsedMessage);
		break;
	case 'error':
		if (state == I_AM_STARTING) {
			setState(I_CAN_START);
		}
		onError("Error message from server: " + parsedMessage.message);
		break;
	case 'iceCandidate':
		webRtcPeer.addIceCandidate(parsedMessage.candidate, function(error) {
			if (error) {
				console.error("Error adding candidate: " + error);
				return;
			}
		});
		break;
	case 'playEnd':
		playEnd();
		break;
	default:
		if (state == I_AM_STARTING) {
			setState(I_CAN_START);
		}
	onError('Unrecognized message', parsedMessage);
	}
}

function start() {
	console.log("Starting video call ...")
	// Disable start button
	setState(I_AM_STARTING);
	showSpinner(videoOutput);

	var dataChannelReceive = document.getElementById('dataChannelReceive');

	function onMessage(event) {
		console.log("Received data " + event["data"]);
		dataChannelReceive.value = event["data"];
	}

	console.log("Creating WebRtcPeer and generating local sdp offer ...");

	var options = {
			remoteVideo : videoOutput,
			dataChannelConfig: {
				id : getChannelName(),
				onmessage : onMessage,
			},
			dataChannels : true,
			onicecandidate : onIceCandidate
	}

	webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options,
			function(error) {
		if (error) {
			return console.error(error);
		}
		webRtcPeer.generateOffer(onOffer);
	});
}

function onOffer(error, offerSdp) {
	if (error)
		return console.error("Error generating the offer");
	console.info('Invoking SDP offer callback function ' + location.host);
	var message = {
			id : 'start',
			sdpOffer : offerSdp
	}
	sendMessage(message);
}

function onError(error) {
	console.error(error);
}

function onIceCandidate(candidate) {
	console.log("Local candidate" + JSON.stringify(candidate));

	var message = {
			id : 'onIceCandidate',
			candidate : candidate
	};
	sendMessage(message);
}

function startResponse(message) {
	setState(I_CAN_STOP);
	console.log("SDP answer received from server. Processing ...");

	webRtcPeer.processAnswer(message.sdpAnswer, function(error) {
		if (error)
			return console.error(error);
	});
}

function stop() {
	console.log("Stopping video call ...");
	setState(I_CAN_START);
	if (webRtcPeer) {

		webRtcPeer.dispose();
		webRtcPeer = null;

		var message = {
				id : 'stop'
		}
		sendMessage(message);
	}
	hideSpinner(videoOutput);
}

function playEnd() {
	setState(I_CAN_START);
	hideSpinner(videoOutput);
}

function setState(nextState) {
	switch (nextState) {
	case I_CAN_START:
		$('#start').attr('disabled', false);
		$("#start").attr('onclick', 'start()');
		$('#stop').attr('disabled', true);
		$("#stop").removeAttr('onclick');
		break;

	case I_CAN_STOP:
		$('#start').attr('disabled', true);
		$('#stop').attr('disabled', false);
		$("#stop").attr('onclick', 'stop()');
		break;

	case I_AM_STARTING:
		$('#start').attr('disabled', true);
		$("#start").removeAttr('onclick');
		$('#stop').attr('disabled', true);
		$("#stop").removeAttr('onclick');
		break;

	default:
		onError("Unknown state " + nextState);
	return;
	}
	state = nextState;
}

function sendMessage(message) {
	var jsonMessage = JSON.stringify(message);
	console.log('Sending message: ' + jsonMessage);
	ws.send(jsonMessage);
}

function showSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].poster = './img/transparent-1px.png';
		arguments[i].style.background = "center transparent url('./img/spinner.gif') no-repeat";
	}
}

function hideSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].src = '';
		arguments[i].poster = './img/webrtc.png';
		arguments[i].style.background = '';
	}
}
/**
 * Lightbox utility (to display media pipeline image in a modal dialog)
 */
$(document).delegate('*[data-toggle="lightbox"]', 'click', function(event) {
	event.preventDefault();
	$(this).ekkoLightbox();
});
