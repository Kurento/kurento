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

const pubNubOptions = {
	channel : 'kurento-videophone-test',
	publish_key : 'pub-c-25965aa1-5d65-410b-b21d-fd90159adf0e',
	subscribe_key : 'sub-c-cb63e056-f08c-11e3-a672-02ee2ddab7fe'
}
const ws_uri = 'ws://localhost:8888/kurento';

var videoInput;
var videoOutput;
var webRtcPeer;
var softphone;

window.onload = function() {
	console = new Console('console', console);
	dragDrop.initElement('videoSmall');
	videoInput = document.getElementById('videoInput');
	videoOutput = document.getElementById('videoOutput');
	softphone = new SoftphonePubnub(videoInput, videoOutput, pubNubOptions);

	document.getElementById('name').focus();
}

function register() {
	var name = document.getElementById('name').value;
	softphone.register(name);
	console.info("User '" + name + "' registered");
}

function call() {
	showSpinner(videoInput, videoOutput);
	
	var peer = document.getElementById('peer').value;
	softphone.call(peer);
	console.info("Calling user '" + peer + "'");
}

function stop() {
	if (webRtcPeer) {
		webRtcPeer.dispose();
	}
	videoInput.src = '';
	videoOutput.src = '';
	hideSpinner(videoInput, videoOutput);
}

// Process request messages
function onIncommingCall(request) {
	var params = request.params;
	var from = params.from;
	var sinkId = params.endpoint;
	
	if (confirm('User ' + from + ' is calling you. Do you accept the call?')) {
		showSpinner(videoInput, videoOutput);
		createPeer(function(error, kurentoClient, src) {
			if (error) return onError(error);
	
			var response = {
				dest : from,
				endpoint : src.id
			}
			request.reply(null, response);
	
			// Send our video to the caller
			connectEndpoints(kurentoClient, src, sinkId);
		});
	}
}

// Private functions
function createPeer(callback) {
	webRtcPeer = kurentoUtils.WebRtcPeer.startSendRecv(videoInput, videoOutput, function(offer) {
		console.log('Invoking SDP offer callback function');

		kurentoClient(ws_uri, function(error, kurentoClient) {
			if (error) return onError(error);

			// Create pipeline
			kurentoClient.create('MediaPipeline', function(error, pipeline) {
				if (error) return onError(error);

				// Create pipeline media elements
				pipeline.create('WebRtcEndpoint', function(error, webRtc) {
					if (error)  return onError(error);

					// Connect the pipeline to the WebRtcPeer client
					webRtc.processOffer(offer, function(error, answer) {
						if (error) return onError(error);

						webRtcPeer.processSdpAnswer(answer);
					});

					callback(null, kurentoClient, webRtc);
				});
			});
		}, onerror);
	});
}

function connectEndpoints(kurentoClient, src, sinkId) {
	kurentoClient.getMediaobjectById(sinkId, function(error, sink) {
		if (error) return onError(error);

		src.connect(sink, function(error) {
			if (error) return onError(error);

			console.log('Connection established');
		});
	})
}

function SoftphonePubnub(videoInput, videoOutput, options) {
	self.options = options
	var rpc;

	this.register = function(peerID, options) {
		function onRequest(request) {
			if (request.params.dest != peerID)
				return;

			switch (request.method) {
			case 'call':
				onIncommingCall(request);
				break;

			default:
				console.error('Unrecognized request', request);
			}
		}

		options = options || self.options;
		var channel = options.channel;
		var rpcOptions = {
			peerID : peerID,
			request_timeout : 10 * 1000
		}

		rpc = new RpcBuilder(RpcBuilder.packers.JsonRPC, rpcOptions);
		var pubnub = PUBNUB.init(options);

		pubnub.subscribe({
			channel : channel,
			message : function(message) {
				var request = rpc.decode(message);
				if (request) onRequest(request);
			}
		});

		rpc.transport = function(message) {
			pubnub.publish({
				channel : channel,
				message : message
			});
		}
	}

	this.call = function(dest) {
		createPeer(function(error, kurentoClient, src) {
			if (error) return onError(error);

			var params = {
				dest : dest,
				endpoint : src.id
			}

			rpc.encode('call', params, function(error, result) {
				if (error) return onError(error);

				var sinkId = result.endpoint;

				// Send our video to the callee
				connectEndpoints(kurentoClient, src, sinkId);
			});
		});
	}
}

function onError(error) {
	console.error(error);
}

function showSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].poster = './img/transparent-1px.png';
		arguments[i].style.background = 'center transparent url("./img/spinner.gif") no-repeat';
	}
}

function hideSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].poster = './img/webrtc.png';
		arguments[i].style.background = '';
	}
}

$(document).delegate('*[data-toggle="lightbox"]', 'click', function(event) {
	event.preventDefault();
	$(this).ekkoLightbox();
});
