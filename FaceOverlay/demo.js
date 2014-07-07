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

const hat_uri =	"http://files.kurento.org/imgs/santa-hat.png"; //requires Internet connectivity


window.addEventListener("load", function(event) {
	console.log("onLoad");
	var button = document.getElementById("startButton");
	button.addEventListener("click", startVideo);
});

function startVideo() {
	console.log("WebRTC loopback starting");
	var videoInput = document.getElementById("videoInput");
	var videoOutput = document.getElementById("videoOutput");

	var webRtcPeer = kwsUtils.WebRtcPeer.startSendRecv(videoInput, videoOutput,
			onOffer, onError);

	function onOffer(offer) {
		console.log("onOffer");
		KwsMedia(ws_uri, function(kwsMedia) {

			kwsMedia.create("MediaPipeline", function(error, pipeline) {
				if (error)
					onError(error);

				console.log("Got MediaPipeline");

				document.getElementById("stopButton").addEventListener("click", function(event){
					pipeline.release();
					webRtcPeer.dispose();
					videoInput.src="";
					videoOutput.src="";
				});

				pipeline.create("WebRtcEndpoint", function(error, webRtc) {
					if (error)
						onError(error);

					console.log("Got WebRtcEndpoint");

					pipeline.create("FaceOverlayFilter",
							function(error, filter) {
								if (error)
									onError(error);

								console.log("Got FaceOverlayFilter");
								var offsetXPercent = -0.4;
								var offsetYPercent = -1;
								var widthPercent = 1.5;
								var heightPercent = 1.5;

								console.log("Setting overlay image");
								filter.setOverlayedImage(hat_uri, offsetXPercent,
										offsetYPercent, widthPercent,
										heightPercent, function(error) {
											if (error)
												onError(error);
											console.log("Set overlay image");
										});

								console.log("Connecting ...");
								webRtc.connect(filter, function(error) {
									if (error)
										onError(error);

									console.log("WebRtcEndpoint --> filter");

									filter.connect(webRtc, function(error) {
										if (error)
											onError(error);

										console.log("Filter --> WebRtcEndpoint");
									});
								});

								webRtc.processOffer(offer, function(error,
										answer) {
									if (error)
										onError(error);

									console.log("SDP answer obtained. Processing ...");
									webRtcPeer.processSdpAnswer(answer);

								});

							});
				});
			});

		});
	};
};

function onError(error) {
	console.log(error);
};
