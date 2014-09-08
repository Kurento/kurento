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

const ws_uri = 'ws://' + location.hostname + ':8888/kurento';

var videoInput;
var videoOutput;
var webRtcPeer;

window.onload = function() {
	console = new Console('console', console);
	videoInput = document.getElementById('videoInput');
	videoOutput = document.getElementById('videoOutput');
}

function start() {
	showSpinner(videoInput, videoOutput);
	webRtcPeer = kurentoUtils.WebRtcPeer.startSendRecv(videoInput, videoOutput, onOffer, onError);
}

function stop() {
	if (webRtcPeer) {
		webRtcPeer.dispose();
	}
	videoInput.src = '';
	videoOutput.src = '';
	hideSpinner(videoInput, videoOutput);
}

function onOffer(sdpOffer){
	kurentoClient(ws_uri, function(error, kurentoClient) {
		if(error) return onError(error);

		kurentoClient.create("MediaPipeline", function(error, pipeline) {
			if(error) return onError(error);

			pipeline.create("WebRtcEndpoint", function(error, webRtc){
				if(error) return onError(error);

				webRtc.processOffer(sdpOffer, function(error, sdpAnswer){
					if(error) return onError(error);

					webRtcPeer.processSdpAnswer(sdpAnswer);
				});

				webRtc.connect(webRtc, function(error){
					if(error) return onError(error);

					console.log("Loopback established");
				});
			});
		});
	});
}

function onError(error){
	console.error(error);
}

function showSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].poster = './img/transparent-1px.png';
		arguments[i].style.background = "center transparent url('./img/spinner.gif') no-repeat";
	}
}

function hideSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].poster = './img/webrtc.png';
		arguments[i].style.background = '';
	}
}

$(document).delegate('*[data-toggle="lightbox"]', 'click', function(event) {
	event.preventDefault();
	$(this).ekkoLightbox();
});
