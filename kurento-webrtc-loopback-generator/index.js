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

/*******************************************************************************
 * Activate "Experimental Javascript" in chrome to have this example working
 *
 * //chrome://flags/ flags (#nable-javascript-harmony)
 *
 ******************************************************************************/

const MEDIA_SERVER_HOSTNAME = location.hostname;
const ws_uri = 'ws://' + MEDIA_SERVER_HOSTNAME + ':8888/kurento';

var webRtcPeer;
var pipeline;

window.addEventListener("load", function(event){
	console.log("onLoad");
	var button = document.getElementById("startButton");
	button.addEventListener("click", startVideo);
});

function startVideo(){
	console.log("Starting WebRTC loopback ...");

	var videoInput = document.getElementById("videoInput");
	var videoOutput = document.getElementById("videoOutput");
	var stopButton = document.getElementById("stopButton");

	webRtcPeer = kurentoUtils.WebRtcPeer.startSendRecv(videoInput,
			videoOutput, onOffer, onError);

	function onOffer(offer){

		console.log("Creating Kurento client...");

		co(function*(){
			try{
				var client   = yield kurentoClient(ws_uri);
				pipeline = yield client.create("MediaPipeline");
				console.log("MediaPipeline created ...");

				var webRtc = yield pipeline.create("WebRtcEndpoint");
				console.log("WebRtcEndpoint created ...");

				var answer = yield webRtc.processOffer(offer);
				console.log("Got SDP answer ...");
				webRtcPeer.processSdpAnswer(answer);

				yield webRtc.connect(webRtc);
				console.log("loopback established ...");

				stopButton.addEventListener("click", stop);

			} catch(e){
				console.log(e);
			}
		})();
	}
}

function stop() {
	if(pipeline){
		pipeline.release();
		pipeline = null;
	}
	if (webRtcPeer) {
		webRtcPeer.dispose();
		webRtcPeer = null;
	}
}

function onError(error) {
	console.error(error);
	stop();
}
