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

const ws_uri = 'wss://' + location.hostname + ':8433/kurento'; //requires Internet connectivity

const file_name = 'files/recorderScreen.webm';

const file_storage = 'file:///var/www/html/'; //path where to be store media in the server
const file_uri = file_storage+file_name; //file to be stored in media server


window.addEventListener('load', function(event) {
	var startRecordButton = document.getElementById('startRecordButton');
	var playButton = document.getElementById('startPlayButton');
	var stopPlayButton = document.getElementById('stopPlayButton');

	var videoPlayer = document.getElementById('videoPlayer');

	startRecordButton.addEventListener('click', startRecording);
	playButton.addEventListener('click', startPlaying);
	stopPlayButton.addEventListener('click', function(event){
		videoPlayer.src="";
	})
});

function startRecording() {
	console.log("onClick");
	var videoInput = document.getElementById("videoInput");
	var videoOutput = document.getElementById("videoOutput");

	var width, height;
	var resolution = document.getElementById('resolution').value
	switch(resolution)
	{
		case 'VGA':
			width = 640;
			height = 480;
		break;
		case 'HD':
			width = 1280;
			height = 720;
		break;
		case 'Full HD':
			width = 1920;
			height = 1080;
		break;

		default:
			return console.error('Unknown resolution',resolution)
	}

	var selectSource = document.getElementById('selectSource')
	var isWebcam = selectSource.value == 'Webcam'
	var constraints =
	{
		audio : isWebcam,
		video : {
			mandatory: {
				maxWidth: width,
				maxHeight: height,
				maxFrameRate : 15,
				minFrameRate: 15
			}
		}
	};
	if(!isWebcam)
		constraints.video.mandatory.chromeMediaSource = 'screen'

	webRtcPeer = kurentoUtils.WebRtcPeer.startSendRecv(videoInput, videoOutput,
			onOffer, onError, constraints);

	function onOffer(offer) {
		console.log("Offer ...");

		kurentoClient(ws_uri, function(error, client) {
			if (error) return onError(error);

			client.create('MediaPipeline', function(error, pipeline) {
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

						var stopRecorderButton = document.getElementById("stopRecordButton");

						function stopRecording(event){
							recorder.stop();
							pipeline.release();
							webRtcPeer.dispose();

							videoInput.src = "";
							videoOutput.src = "";

							this.removeEventListener('click', stopRecording);
						}

						stopRecorderButton.addEventListener("click", stopRecording);
					});
				});
			});
		});
	}
}


function startPlaying() {

	console.log("Start playing");

	var videoPlayer = document.getElementById('videoPlayer');

	videoPlayer.src = file_name;
}

function onError(error) {
	console.log(error);
}
