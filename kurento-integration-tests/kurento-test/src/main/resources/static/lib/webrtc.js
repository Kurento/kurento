/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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

var local;
var video;
var webRtcPeer;
var sdpOffer;
var userMediaConstraints = {
	fake : true
};
var videoStream = null;
var audioStream = null;
var defaultVideoConstraints = {
	mandatory : {
		maxWidth : 640,
		maxFrameRate : 15,
		minFrameRate : 15
	}
};

try {
	kurentoUtils.WebRtcPeer.prototype.server.iceServers = [];
} catch (e) {
	console.warn(e);
}

window.onload = function() {
	console = new Console("console", console);
	local = document.getElementById("local");
	video = document.getElementById("video");

	setInterval(updateCurrentTime, 100);
}

function setAudioUserMediaConstraints() {
	userMediaConstraints = {
		audio : true,
		video : false,
		fake : true
	};
}

function setVideoUserMediaConstraints() {
	userMediaConstraints = {
		audio : false,
		video : defaultVideoConstraints,
		fake : true
	};
}

function setCustomAudio(audioUrl) {
	mediaConstraints = {
		audio : false,
		video : defaultVideoConstraints,
		fake : true
	};
	getUserMedia(mediaConstraints, function(userStream) {
		videoStream = userStream;
	}, onError);

	var context = new AudioContext();
	var audioTest = document.getElementById("audioTest");
	audioTest.src = audioUrl;
	var sourceStream = context.createMediaElementSource(audioTest);
	var mixedOutput = context.createMediaStreamDestination();
	sourceStream.connect(mixedOutput);
	audioStream = mixedOutput.stream;
}

function startSendRecv() {
	console.log("Starting WebRTC in SendRecv mode...");
	showSpinner(local, video);
	webRtcPeer = kurentoUtils.WebRtcPeer.start('sendRecv', local, video,
			onOffer, onError, userMediaConstraints, videoStream, audioStream);
}

function startSendOnly() {
	console.log("Starting WebRTC in SendOnly mode...");
	showSpinner(local);
	webRtcPeer = kurentoUtils.WebRtcPeer.start('send', local, null, onOffer,
			onError, userMediaConstraints, videoStream, audioStream);
}

function startRecvOnly() {
	console.log("Starting WebRTC in RecvOnly mode...");
	showSpinner(video);
	webRtcPeer = kurentoUtils.WebRtcPeer.start('recv', null, video, onOffer,
			onError, userMediaConstraints, videoStream, audioStream);
}

function onError(error) {
	console.error(error);
}

function onOffer(offer) {
	console.info("SDP offer:");
	sdpOffer = offer;
	console.info(sdpOffer);
}

function processSdpAnswer(answer) {
	var sdpAnswer = window.atob(answer);
	console.info("SDP answer:");
	console.info(sdpAnswer);
	webRtcPeer.processSdpAnswer(sdpAnswer);
}

function updateCurrentTime() {
	document.getElementById("currentTime").value = video.currentTime;
}

function log(text) {
	document.getElementById("status").value = text;
}

function addEventListener(type, callback) {
	video.addEventListener(type, callback, false);
}

function videoEvent(e) {
	if (!e) {
		e = window.event;
	}
	if (e.type == "playing") {
		audioTest.play();
	}
	log(e.type);
}

function addTestName(testName) {
	document.getElementById("testName").innerHTML = testName;
}

function appendStringToTitle(string) {
	document.getElementById("testTitle").innerHTML += " " + string;
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
