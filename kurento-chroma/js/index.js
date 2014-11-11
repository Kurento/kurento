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
const APP_SERVER_HOST = location.host;
const ws_uri = 'ws://' + MEDIA_SERVER_HOSTNAME + ':8888/kurento';
const bg_uri = 'http://' + APP_SERVER_HOST + '/img/mario.jpg';

window.addEventListener("load", function(event)
{
	kurentoClient.register(kurentoModuleChroma)
	console = new Console('console', console);

	var videoInput = document.getElementById('videoInput');
	var videoOutput = document.getElementById('videoOutput');

	var start = document.getElementById("start");
	var stop = document.getElementById("stop");

	start.addEventListener("click", function start()
	{
		console.log("WebRTC loopback starting");

		showSpinner(videoInput, videoOutput);

		var webRtcPeer = kurentoUtils.WebRtcPeer.startSendRecv(videoInput, videoOutput, onOffer, onError);

		function onOffer(sdpOffer) {
			console.log("onOffer");

			kurentoClient(ws_uri, function(error, client) {
				if (error) return onError(error);

				client.create('MediaPipeline', function(error, pipeline) {
					if (error) return onError(error);

					console.log("Got MediaPipeline");

					stop.addEventListener("click", function(event)
					{
						pipeline.release();
						pipeline = null;

						webRtcPeer.dispose();
						webRtcPeer = null;

						hideSpinner(videoInput, videoOutput);
					});

					pipeline.create('WebRtcEndpoint', function(error, webRtc) {
						if (error) return onError(error);

						console.log("Got WebRtcEndpoint");
						pipeline.create('ChromaFilter', {window: {topRightCornerX:5 , topRightCornerY:5 , width:30 , height:30 }}, function(error, filter) {
							if (error) return onError(error);
							
							console.log("Got Filter");

							webRtc.connect(filter, function(error) {
								if (error) return onError(error);

								console.log("WebRtcEndpoint --> filter");
								filter.connect(webRtc, function(error) {
									if (error) return onError(error);

									console.log("filter --> WebRtcEndpoint");
								});

								filter.setBackground (bg_uri, function(error) {
									if (error) return onError(error);

									console.log("Set Image");
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


function onError(error) {
	if(error) console.error(error);
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
