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

window.addEventListener("load", function(event){
	console.log("onLoad");
	var button = document.getElementById("startButton");
	button.addEventListener("click", startVideo);
});

function startVideo(){
	console.log("WebRTC loopback starting ...");
	var videoInput = document.getElementById("videoInput");
	var videoOutput = document.getElementById("videoOutput");

	var webRtcPeer = kwsUtils.WebRtcPeer.startSendRecv(videoInput, videoOutput,
			onOffer, onError);

	function onOffer(offer){
		console.log("Creating KwsMedia ...");
		KwsMedia(ws_uri, function(kwsMedia){
			kwsMedia.create("MediaPipeline", function(error, pipeline){
				if(error) return onError(error);

				document.getElementById("stopButton").addEventListener("click", function(event){
					pipeline.release();
					webRtcPeer.dispose();
					videoInput.src = "";
					videoOutput.src = "";
				});

				console.log("MediaPipeline created ...");

				pipeline.create("WebRtcEndpoint", function(error, webRtc){
					if(error) return onError(error);

					console.log("WebRtcEndpoint created ...");

					webRtc.processOffer(offer, function(error, answer){
						if(error) return onError(error);

						webRtcPeer.processSdpAnswer(answer);

					});

					webRtc.connect(webRtc, function(error){
						if(error) return onError(error);

						console.log("loopback established ...");

					});
				});
			});
		});
	};
};


function onError(error){
	console.log(error);
};
