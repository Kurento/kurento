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

const ws_uri = 'ws://demo01.kurento.org:8888/thrift/ws/websocket'; //requires Internet connectivity
const hat_uri =	'http://files.kurento.org/imgs/santa-hat.png'; //requires Internet connectivity

var videoInput = document.getElementById('videoInput');
var videoOutput = document.getElementById('videoOutput');
var webRtcPeer;

window.onload = function() {
	console = new Console('console', console);
}

function start() {
	showSpinner(videoInput, videoOutput);
	webRtcPeer = kwsUtils.WebRtcPeer.startSendRecv(videoInput, videoOutput, onOffer, onError);
	function onOffer(offer) {
		KwsMedia(ws_uri, function(error, kwsMedia) {
			if (error) return onError(error);

			kwsMedia.create('MediaPipeline', function(error, pipeline) {
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
}

function stop() {
	if (webRtcPeer) {
		webRtcPeer.dispose();
	}
	videoInput.src = '';
	videoOutput.src = '';
	hideSpinner(videoInput, videoOutput);
}

function onError(error) {
	console.log(error);
}

function showSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].poster = 'http://files.kurento.org/imgs/transparent-1px.png';
		arguments[i].style.background = "center transparent url('http://files.kurento.org/imgs/spinner.gif') no-repeat";
	}
}

function hideSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].poster = '';
		arguments[i].style.background = '';
	}
}
