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

const MEDIA_SERVER_HOSTNAME = location.hostname;
const ws_uri = 'ws://' + MEDIA_SERVER_HOSTNAME + ':8888/kurento';
const file_uri = 'file:///tmp/kurento-hello-world-recording.webm';

var videoInput;
var videoOutput;
var webRtcPeer;
var client;
var pipeline;

const IDLE = 0;
const DISABLED = 1;
const CALLING = 2;
const PLAYING = 3;

function setStatus(nextState){
	switch(nextState){
		case IDLE:
			$('#start').attr('disabled', false)
			$('#stop').attr('disabled', true)
			$('#play').attr('disabled', false)
			break;
		case DISABLED:
			$('#start').attr('disabled', true)
			$('#stop').attr('disabled', true)
			$('#play').attr('disabled', true)
			break;
		case CALLING:
			$('#start').attr('disabled', true)
			$('#stop').attr('disabled', false)
			$('#play').attr('disabled', true)
			break;
		case PLAYING:
			$('#start').attr('disabled', true)
			$('#stop').attr('disabled', false)
			$('#play').attr('disabled', true)
			break;
		default:
			return;
	}
}

window.onload = function() {
	console = new Console('console', console);
	videoInput = document.getElementById('videoInput');
	videoOutput = document.getElementById('videoOutput');
	setStatus(IDLE);
}

function start() {
	setStatus(DISABLED);
	showSpinner(videoInput, videoOutput);
	webRtcPeer = kurentoUtils.WebRtcPeer.startSendRecv(videoInput, videoOutput, onStartOffer, onError);
}

function stop() {
	if (webRtcPeer) {
		webRtcPeer.dispose();
		webRtcPeer = null;
	}
	if(pipeline){
		pipeline.release();
		pipeline = null;
	}
	hideSpinner(videoInput, videoOutput);
	setStatus(IDLE);
}

function play(){
	setStatus(DISABLED)
	showSpinner(videoOutput);
	webRtcPeer = kurentoUtils.WebRtcPeer.startRecvOnly(videoOutput, onPlayOffer, onError);
}

function onPlayOffer(sdpOffer){
	co(function*(){
		try{
			if(!client)
				client = yield kurentoClient(ws_uri);
			pipeline = yield client.create('MediaPipeline');
			var webRtc = yield pipeline.create('WebRtcEndpoint');
			var player = yield pipeline.create('PlayerEndpoint', {uri : file_uri});

			yield player.connect(webRtc);

			var sdpAnswer = yield webRtc.processOffer(sdpOffer);
			webRtcPeer.processSdpAnswer(sdpAnswer);

			yield player.play()

			player.on('EndOfStream', function(){
				stop();
			});

			setStatus(PLAYING)

		} catch(e){
			onError(e);
		}
	})();
}

function onStartOffer(sdpOffer){

	co(function*(){
			try{
				if(!client)
					client = yield kurentoClient(ws_uri);

				pipeline = yield client.create('MediaPipeline');
				var webRtc = yield pipeline.create('WebRtcEndpoint');
				var recorder = yield pipeline.create('RecorderEndpoint', {uri: file_uri});

				yield webRtc.connect(recorder);
				yield webRtc.connect(webRtc);

				yield recorder.record();

				var sdpAnswer = yield webRtc.processOffer(sdpOffer);

				webRtcPeer.processSdpAnswer(sdpAnswer)

				setStatus(CALLING);

			} catch(e){
				onError(e);
			}
	})();
}

function onError(error) {
	if(error) console.error(error);
	stop();
}

function showSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].poster = 'img/transparent-1px.png';
		arguments[i].style.background = "center transparent url('img/spinner.gif') no-repeat";
	}
}

function hideSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].src = '';
		arguments[i].poster = 'img/webrtc.png';
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
