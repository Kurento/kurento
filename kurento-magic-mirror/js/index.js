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
const hat_uri = 'http://files.kurento.org/imgs/santa-hat.png'; //requires Internet connectivity

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

function onOffer(offer) {
	kurentoClient(ws_uri, function(error, kurentoClient) {
		if (error) return onError(error);

		kurentoClient.create('MediaPipeline', function(error, pipeline) {
			if (error) return onError(error);

			pipeline.create('WebRtcEndpoint', function(error, webRtc) {
				if (error) return onError(error);

				pipeline.create('FaceOverlayFilter', function(error, filter) {
					if (error) return onError(error);

					var offsetXPercent = -0.4;
					var offsetYPercent = -1;
					var widthPercent = 1.5;
					var heightPercent = 1.5;
					filter.setOverlayedImage(hat_uri, offsetXPercent,
						offsetYPercent, widthPercent,
						heightPercent, function(error) {
							if (error) return onError(error);
						});

					webRtc.connect(filter, function(error) {
						if (error) return onError(error);

						filter.connect(webRtc, function(error) {
							if (error) return onError(error);
						});
					});

					webRtc.processOffer(offer, function(error, answer) {
						if (error) return onError(error);

						webRtcPeer.processSdpAnswer(answer);
					});
				});
			});
		});
	});
}

function onError(error) {
	console.error(error);
}

function showSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].poster = 'http://files.kurento.org/imgs/transparent-1px.png';
		arguments[i].style.background = "center transparent url('http://files.kurento.org/imgs/spinner.gif') no-repeat";
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
