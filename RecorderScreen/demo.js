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
const file_storage = 'file:///var/www/html/files/'; //path where to be store media in the server
const file_uri = file_storage+'recorder_demo.webm'; //file to be stored in media server

const constraintsWebcam =
{
	audio : true,
	video : {
		mandatory: {
			maxWidth: 640,
			maxFrameRate : 15,
			minFrameRate: 15
		}
	}
}

const constraintsDesktop =
{
	video : {
		mandatory: {
			chromeMediaSource: 'screen',
			maxFrameRate : 15,
			minFrameRate: 15
		}
	}
}


window.addEventListener('load', function(event) {
	var startRecordButton = document.getElementById('startRecordButton');
	var playButton = document.getElementById('startPlayButton');

	startRecordButton.addEventListener('click', startRecording);
	playButton.addEventListener('click', startPlaying);
});


function startRecording() {
	console.log("onClick");
	var videoInput = document.getElementById("videoInput");
	var videoOutput = document.getElementById("videoOutput");

	var constraints;
	if(document.getElementById('selectSource').value == 'Desktop')
		constraints = constraintsDesktop
	else
		constraints = constraintsWebcam

	webRtcPeer = kwsUtils.WebRtcPeer.startSendRecv(videoInput, videoOutput,
			onOffer, onError, constraints);

	function onOffer(offer) {
		console.log("Offer ...");

		KwsMedia(ws_uri, function(kwsMedia) {
			kwsMedia.create('MediaPipeline', function(error, pipeline) {
				if (error) return onError(error);
				console.log("Got MediaPipeline");

				pipeline.create('WebRtcEndpoint', function(error, webRtc) {
					if (error) return onError(error);
					console.log("Got WebRtcEndpoint");

					// Set video loopback
					webRtc.connect(webRtc, function(error) {
						if (error) return onError(error);
						console.log("Second connect");
					});

					webRtc.processOffer(offer, function(error, answer) {
						if (error) return onError(error);
						console.log("offer");

						webRtcPeer.processSdpAnswer(answer);
					});

					pipeline.create('RecorderEndpoint', {uri : file_uri}, function(error, recorder) {
						if (error) return onError(error);
						console.log("Got RecorderEndpoint");

						webRtc.connect(recorder, function(error) {
							if (error) return onError(error);
							console.log("Connected");
						});

						recorder.record(function(error) {
							if (error) return onError(error);
							console.log("recording");
						});

						document.getElementById("stopRecordButton").addEventListener("click", function(event){
							recorder.stop();
							pipeline.release();
							webRtcPeer.dispose();

							videoInput.src = "";
							videoOutput.src = "";
						})
					});
				});
			});
		});
	}
}


function startPlaying() {

	console.log("Start playing");

	var videoPlayer = document.getElementById('videoPlayer');

	function onPlayOffer(offer) {
		KwsMedia(ws_uri, function(kwsMedia) {
			kwsMedia.create('MediaPipeline', function(error, pipeline) {
				if (error) return onError(error);

				pipeline.create("PlayerEndpoint", { uri : file_uri }, function(error, player) {
					if (error) return onError(error);

					player.on('EndOfStream', function(event){
						pipeline.release();
						videoPlayer.src = "";
					});

					pipeline.create('HttpGetEndpoint', function(error, httpGet) {
						if (error) return onError(error);

						player.connect(httpGet, function(error) {
							if (error) return onError(error);

							console.log("Player connected");
						});

						// Set the video on the video tag
						httpGet.getUrl(function(error, url)
						{
							if(error) return onerror(error);

							videoPlayer.src = url;

							console.log(url);

							// Start player
							player.play(function(error)
							{
								if (error) return onError(error);

								console.log("Playing ...");
							});
						});
					});
				});

				document.getElementById("stopPlayButton").addEventListener("click", function(event){
					pipeline.release();

					videoPlayer.src="";
				})
			});
		});
	};
}

function onError(error) {
	console.log(error);
}
