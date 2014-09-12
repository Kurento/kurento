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

var kurentoClient;
var videoInput;
var videoOutput;
var webRtcPeer;

var plumberSrc;

function connect(p1, p2, callback) {
	p2.getAddress(function (error, address) {
		if(error) return onError(error);

		console.log("Got address: " + address);

		p2.getPort (function (error, port) {
			if(error) return onError(error);

			console.log("Got port: " + port);

			p1.link (address, port, function(error, success) {
				callback (error, success);
			});
		});
	});
}

window.onload = function() {
	console = new Console('console', console);
	videoInput = document.getElementById('videoInput');
	videoOutput = document.getElementById('videoOutput');

	kurentoClient(ws_uri, function(error, client) {
	  if(error) return onError(error);

	  kurentoClient = client;
	});
}

function start() {
	showSpinner(videoInput, videoOutput);
	console.log("Creating sink pipeline");

	kurentoClient.create("MediaPipeline", function(error, pipeline) {
		if(error) return onError(error);

		pipeline.create("PlumberEndpoint", function(error, plumberEndPoint) {
			if(error) return onError(error);

			plumberSrc = plumberEndPoint;
			console.log("PlumberEndPoint created");

			pipeline.create('HttpGetEndpoint', function(error, httpGetEndpoint) {
			  if (error) return onError(error);

				console.log("HttpGetEndPoint created")
				plumberEndPoint.connect(httpGetEndpoint, function(error) {
					if (error) return onError(error);

					httpGetEndpoint.getUrl(function(error, url) {
						if (error) return onError(error);

						console.log ("Getting media from url: " + url);
						videoOutput.src = url;
						webRtcPeer = kurentoUtils.WebRtcPeer.startSendOnly(videoInput, onOffer, onError);
					});
				});
			});
		});
	});
}

function stop() {
	if (webRtcPeer) {
		webRtcPeer.dispose();
		webRtcPeer = null;
	}
	videoInput.src = '';
	videoOutput.src = '';
	hideSpinner(videoInput, videoOutput);
}

function onOffer(sdpOffer){
    console.log ("Offer received.");
    console.log ("Creating source pipeline...");
	kurentoClient.create("MediaPipeline", function(error, pipeline) {
		if(error) return onError(error);

		pipeline.create("WebRtcEndpoint", function(error, webRtc) {
			if(error) return onError(error);

			console.log ("Created webRtc");

			webRtc.processOffer(sdpOffer, function(error, sdpAnswer) {
				if(error) return onError(error);

				webRtcPeer.processSdpAnswer(sdpAnswer);
			});

			pipeline.create("PlumberEndpoint", function(error, plumberSink) {
				if(error) return onError(error);

				console.log("PlumberEndPoint created");

				// Connect both plumberendpoints
				connect (plumberSink, plumberSrc, function(error, success) {
					if(error) return onError(error);
					if (!success) {
						console.error("Can not connect plumber end points");
						return;
					}

					console.log ("Pipelines connected");

					webRtc.connect(plumberSink, function(error){
						if(error) return onError(error);

						console.log("Source pipeline created.");
					});
				});
			});
		});
	});
}

function onError(error){
	console.error(error);
}

function showSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].poster = './img/transparent-1px.png';
		arguments[i].style.background = "center transparent url('./img/spinner.gif') no-repeat";
	}
}

function hideSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].poster = './img/webrtc.png';
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
