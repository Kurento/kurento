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

const MEDIA_SERVER_HOST = location.hostname;
const APP_SERVER_HOST = location.hostname;

const ws_uri = 'ws://' + MEDIA_SERVER_HOST + ':8888/kurento';
const hat_uri = 'http://' + APP_SERVER_HOST + ':' + location.port + '/img/mario-wings.png';

var pipeline;
var webRtcPeer

window.addEventListener("load", function(event)
{
	console = new Console('console', console);

	var videoInput = document.getElementById('videoInput');
	var videoOutput = document.getElementById('videoOutput');

	var start = document.getElementById("start");
	var stop = document.getElementById("stop");
	stop.addEventListener("click", stop);

	start.addEventListener("click", function start()
	{
		console.log("WebRTC loopback starting");

		showSpinner(videoInput, videoOutput);

		webRtcPeer = kurentoUtils.WebRtcPeer.startSendRecv(videoInput, videoOutput, onOffer, onError);

		function onOffer(sdpOffer) {
			console.log("onOffer");

			kurentoClient(ws_uri, function(error, client) {
				if (error) return onError(error);

				client.create('MediaPipeline', function(error, p) {
					if (error) return onError(error);

					pipeline = p;

					console.log("Got MediaPipeline");

					pipeline.create('WebRtcEndpoint', function(error, webRtc) {
						if (error) return onError(error);

						console.log("Got WebRtcEndpoint");

						pipeline.create('FaceOverlayFilter', function(error, filter) {
							if (error) return onError(error);

							console.log("Got FaceOverlayFilter");

							var offsetXPercent = -0.35;
							var offsetYPercent = -1.2;
							var widthPercent = 1.6;
							var heightPercent = 1.6;

							console.log("Setting overlay image");

							filter.setOverlayedImage(hat_uri, offsetXPercent,
								offsetYPercent, widthPercent,
								heightPercent, function(error) {
									if (error) return onError(error);

									console.log("Set overlay image");
								});

							console.log("Connecting ...");

							webRtc.connect(filter, function(error) {
								if (error) return onError(error);

								console.log("WebRtcEndpoint --> filter");

								filter.connect(webRtc, function(error) {
									if (error) return onError(error);

									console.log("Filter --> WebRtcEndpoint");
								});
							});

							webRtc.processOffer(sdpOffer, function(error, sdpAnswer) {
								if (error) return onError(error);

								console.log("SDP answer obtained. Processing ...");

								webRtcPeer.processSdpAnswer(sdpAnswer);
							});
						});
					});
				});
			});
		}
	});
});

function stop(){
	if(pipeline){
		pipeline.release();
		pipeline = null;
	}

	if(webRtcPeer){
		webRtcPeer.dispose();
		webRtcPeer = null;
	}

	videoInput.src="";
	videoOutput.src="";

	hideSpinner(videoInput, videoOutput);
}

function onError(error) {
	if(error) console.error(error);
	stop()
}

function showSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].poster = 'http://files.kurento.org/imgs/transparent-1px.png';
		arguments[i].style.background = "center transparent url('http://files.kurento.org/imgs/spinner.gif') no-repeat";
	}
}

function hideSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].poster = 'img/webrtc.png';
		arguments[i].style.background = '';
	}
}

$(document).delegate('*[data-toggle="lightbox"]', 'click', function(event) {
	event.preventDefault();
	$(this).ekkoLightbox();
});
