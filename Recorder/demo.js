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

const file_uri = 'file:///tmp/recorder_demo.webm'; //file to be stored in media server

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

	webRtcPeer = kwsUtils.WebRtcPeer.startSendRecv(videoInput, videoOutput,
			onOffer, onError);

	function onOffer(offer) {
		console.log("Offer ...");
		KwsMedia(ws_uri, function(kwsMedia) {

			kwsMedia.create('MediaPipeline', function(error, pipeline) {
				if (error)
					onError(error);
				console.log("Got MediaPipeline");
				pipeline.create('RecorderEndpoint', {uri : file_uri}, function(error, recorder) {
					if (error)
						onError(error);

					console.log("Got RecorderEndpoint");
					pipeline.create('WebRtcEndpoint', function(error, webRtc) {
						if (error)
							onError(error);
						console.log("Got WebRtcEndpoint");
						webRtc.connect(recorder, function(error) {
							if (error)
								onError(error);
							console.log("Connected");
							recorder.record(function(error) {
								if (error)
									onError(error);
								console.log("record");

								webRtc.connect(webRtc, function(error) {
									if (error)
										onError(error);
									console.log("Second connect");
								});

								webRtc.processOffer(offer, function(error,
										answer) {
									if (error)
										onError(error);
									console.log("offer");
									webRtcPeer.processSdpAnswer(answer);
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
			});
		});
	}
}


function startPlaying() {

	console.log("Start playing");

	var videoPlayer = document.getElementById('videoPlayer');
	var webRtcPeer = kwsUtils.WebRtcPeer.startRecvOnly(videoPlayer,
			onPlayOffer, onError);

	function onPlayOffer(offer) {
		KwsMedia(ws_uri, function(kwsMedia) {
			kwsMedia.create('MediaPipeline', function(error, pipeline) {
				pipeline.create('WebRtcEndpoint', function(error, webRtc) {
					webRtc.processOffer(offer, function(error, answer) {
						webRtcPeer.processSdpAnswer(answer);

						pipeline.create("PlayerEndpoint", {
							uri : file_uri
						}, function(error, player) {

							player.on('EndOfStream', function(event){
								pipeline.release();
								videoPlayer.src = "";
							});

							player.connect(webRtc, function(error) {
								if (error)
									onError(error);
								player.play(function(error) {
									if (error)
										onError(error);
									console.log("Playing ...");
								});
							});

							document.getElementById("stopPlayButton").addEventListener("click", function(event){
								pipeline.release();
								webRtcPeer.dispose();
								videoPlayer.src="";
							})
						});
					});
				});
			});
		});
	};
}

function onError(error) {
	console.log(error);
}
