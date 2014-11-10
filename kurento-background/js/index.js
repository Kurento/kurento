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
const file_uri_1 = 'http://' + APP_SERVER_HOST + '/img/fiwarecut_30.webm';
const file_uri_2 = 'http://' + APP_SERVER_HOST + '/img/sintel.webm';
const file_uri_3 = 'http://' + APP_SERVER_HOST + '/img/Galapagos.webm';
const file_uri_4 = 'http://' + APP_SERVER_HOST + '/img/kinect.webm';
var sample1, sample2, sample3, sample4;

kurentoClient.register(kurentoModuleBackgroundextractor)

var videoOutput;
var webRtcPeer, webRtcPeerSample1, webRtcPeerSample2, webRtcPeerSample3, webRtcPeerSample4;
var webRtcSample1, webRtcSample2, webRtcSample3, webRtcSample4;
var playerEndpointSample1, playerEndpointSample2, playerEndpointSample3, playerEndpointSample4;
var pipeline;
var video_port;
var alphaBlending;

window.onload = function() {
	console = new Console('console', console);
	videoOutput = document.getElementById('videoOutput');
	sample1 = document.getElementById('sample1');
	sample2 = document.getElementById('sample2');
	sample3 = document.getElementById('sample3');
	sample4 = document.getElementById('sample4');

	$('#stop').attr('disabled', true);
	$('#start').attr('disabled', false);
	$('#noMore').attr('disabled', true);
}

function start() {
	showSpinner(videoOutput, sample1, sample2, sample3, sample4);
	webRtcPeer = kurentoUtils.WebRtcPeer.startSendRecv(null, videoOutput, onOffer, onError);

	$('#stop').attr('disabled', false);
	$('#start').attr('disabled', true);	
	$('#noMore').attr('disabled', false);
}

function stop() {
	if (webRtcPeer) {
		webRtcPeer.dispose();
		webRtcPeer = null;
	}

	if (webRtcPeerSample1) {
		webRtcPeerSample1.dispose ();
		webRtcPeerSample1 = null;
		playerEndpointSample1.stop ();
		playerEndpointSample1.release ();
		webRtcSample1.release ();
	}

	if (webRtcPeerSample2) {
		webRtcPeerSample2.dispose ();
		webRtcPeerSample2 = null;
		playerEndpointSample2.stop ();
		playerEndpointSample2.release ();
		webRtcSample2.release ();
	}

	if (webRtcPeerSample3) {
		webRtcPeerSample3.dispose ();
		webRtcPeerSample3 = null;
		playerEndpointSample3.stop ();
		playerEndpointSample3.release ();
		webRtcSample3.release ();
	}	

	if (webRtcPeerSample4) {
		webRtcPeerSample4.dispose ();
		webRtcPeerSample4 = null;
		playerEndpointSample4.stop ();
		playerEndpointSample4.release ();
		webRtcSample4.release ();
	}

	videoOutput.src = '';
	hideSpinner(videoOutput, sample1, sample2, sample3, sample4);
	if(pipeline){
		pipeline.release();
		pipeline = null;
	}

	$('#stop').attr('disabled', true);
	$('#start').attr('disabled', false);
	$('#noMore').attr('disabled', true);
}

function onOffer(sdpOffer) {
    kurentoClient(ws_uri, function(error, kurentoClient) {
		if (error) return onError(error);

		kurentoClient.create('MediaPipeline', function(error, _pipeline) {
		    if (error) return onError(error);

		    pipeline = _pipeline;	    
			webRtcPeerSample1 = kurentoUtils.WebRtcPeer.startRecvOnly(sample1, onOfferSample1, onError);
			webRtcPeerSample2 = kurentoUtils.WebRtcPeer.startRecvOnly(sample2, onOfferSample2, onError);
			webRtcPeerSample3 = kurentoUtils.WebRtcPeer.startRecvOnly(sample3, onOfferSample3, onError);
			webRtcPeerSample4 = kurentoUtils.WebRtcPeer.startRecvOnly(sample4, onOfferSample4, onError);
		    pipeline.create('WebRtcEndpoint', function(error, webRtc) {
				if (error) return onError(error);
			    
			    pipeline.create('AlphaBlending', function(error, _alphaBlending) {
					if(error) return onError(error);

					alphaBlending = _alphaBlending;
		    		alphaBlending.createHubPort (function(error, webRtc_port) {
						if (error) return onError(error);

						pipeline.create ('BackgroundExtractorFilter', function(error, background) {
						    if (error) return onError(error);

						    webRtc.connect(background, function(error) {
								if (error) return onError(error);

								background.connect(webRtc_port, function(error) {
							    if (error) return onError(error);

									webRtc_port.connect(webRtc, function(error) {
								    	if (error) return onError(error);

									    alphaBlending.setMaster (webRtc_port, 3, function(error) {
										if (error) return onError(error);

											console.log("Set Master Port");
									    });

										webRtc.processOffer(sdpOffer, function(error, sdpAnswer) {
											if (error) return onError(error);

											webRtcPeer.processSdpAnswer(sdpAnswer);
										});
									});
								});
							});
						});
					});
				});
		    });
		});
    });
}

function onOfferSample1(sdpOffer) {
	if (pipeline == null){
		console.log ("MediaPipeline is still not create")
		return;	
	}
	
	pipeline.create('WebRtcEndpoint', function(error, _webRtcSample1) {
		if (error) return onError(error);

		webRtcSample1 = _webRtcSample1;
		pipeline.create('PlayerEndpoint', {uri : file_uri_1}, function(error, _playerEndpointSample1) {
		    if(error) return onError(error);

		    playerEndpointSample1 = _playerEndpointSample1;
		    playerEndpointSample1.connect(webRtcSample1, function(error) {
				if (error) return onError(error);

				playerEndpointSample1.play(function(error){
					if(error) return onError(error);
					
					console.log('Playing Sample 1...');
				});

				playerEndpointSample1.on('EndOfStream', function(data)
			    {		
		    		playerEndpointSample1.play();						
			    });

				webRtcSample1.processOffer(sdpOffer, function(error, sdpAnswer) {
					if (error) return onError(error);
					
					webRtcPeerSample1.processSdpAnswer(sdpAnswer);
				});
			});
		});
	});
}

function onOfferSample2(sdpOffer) {
	if (pipeline == null){
		console.log ("MediaPipeline is still not create")
		return;	
	}
	
	pipeline.create('WebRtcEndpoint', function(error, _webRtcSample2) {
		if (error) return onError(error);

		webRtcSample2 = _webRtcSample2;
		pipeline.create('PlayerEndpoint', {uri : file_uri_2}, function(error, _playerEndpointSample2) {
		    if(error) return onError(error);

		    playerEndpointSample2 = _playerEndpointSample2;
		    playerEndpointSample2.connect(webRtcSample2, function(error) {
				if (error) return onError(error);

				playerEndpointSample2.play(function(error){
					if(error) return onError(error);
					
					console.log('Playing Sample 2...');
				});

				playerEndpointSample2.on('EndOfStream', function(data)
			    {		
		    		playerEndpointSample2.play();							
			    });

				webRtcSample2.processOffer(sdpOffer, function(error, sdpAnswer) {
					if (error) return onError(error);
					
					webRtcPeerSample2.processSdpAnswer(sdpAnswer);
				});
			});
		});
	});
}

function onOfferSample3(sdpOffer) {
	if (pipeline == null){
		console.log ("MediaPipeline is still not create")
		return;	
	}
	
	pipeline.create('WebRtcEndpoint', function(error, _webRtcSample3) {
		if (error) return onError(error);

		webRtcSample3 = _webRtcSample3;
		pipeline.create('PlayerEndpoint', {uri : file_uri_3}, function(error, _playerEndpointSample3) {
		    if(error) return onError(error);

		    playerEndpointSample3 = _playerEndpointSample3;
		    playerEndpointSample3.connect(webRtcSample3, function(error) {
				if (error) return onError(error);

				playerEndpointSample3.play(function(error){
					if(error) return onError(error);
					
					console.log('Playing Sample 3...');
				});

				playerEndpointSample3.on('EndOfStream', function(data)
			    {		
		    		playerEndpointSample3.play();							
			    });

				webRtcSample3.processOffer(sdpOffer, function(error, sdpAnswer) {
					if (error) return onError(error);
					
					webRtcPeerSample3.processSdpAnswer(sdpAnswer);
				});
			});
		});
	});
}

function onOfferSample4(sdpOffer) {
	if (pipeline == null){
		console.log ("MediaPipeline is still not create")
		return;	
	}
	
	pipeline.create('WebRtcEndpoint', function(error, _webRtcSample4) {
		if (error) return onError(error);

		webRtcSample4 = _webRtcSample4;
		pipeline.create('PlayerEndpoint', {uri : file_uri_4}, function(error, _playerEndpointSample4) {
		    if(error) return onError(error);

		    playerEndpointSample4 = _playerEndpointSample4;
		    playerEndpointSample4.connect(webRtcSample4, function(error) {
				if (error) return onError(error);

				playerEndpointSample4.play(function(error){
					if(error) return onError(error);
					
					console.log('Playing Sample 4...');
				});

				playerEndpointSample4.on('EndOfStream', function(data)
			    {		
		    		playerEndpointSample4.play();							
			    });

				webRtcSample4.processOffer(sdpOffer, function(error, sdpAnswer) {
					if (error) return onError(error);
					
					webRtcPeerSample4.processSdpAnswer(sdpAnswer);
				});
			});
		});
	});
}

function onError(error) {
	console.error(error);
	stop();
}

function sample1click() {
	$('#noMore').attr('disabled', false);
	connect (playerEndpointSample1);
}

function sample2click() {
	$('#noMore').attr('disabled', false);
	connect (playerEndpointSample2);
}

function sample3click() {
	$('#noMore').attr('disabled', false);
	connect (playerEndpointSample3);
}

function sample4click() {
	$('#noMore').attr('disabled', false);
	connect (playerEndpointSample4);
}

function connect (samplePlayer) {

	if (video_port != null) {
		video_port.release ();
	}

	alphaBlending.createHubPort (function(error, _video_port) {
    	if (error) return onError(error);
   		
   		video_port = _video_port;
		samplePlayer.connect(video_port, function(error) {
	    	if (error) return onError(error);
		});
    });
}

function noMore (){
	if (video_port != null) {
		video_port.release ();
	}
	$('#noMore').attr('disabled', true);
}

function showSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].poster = 'http://files.kurento.org/imgs/transparent-1px.png';
		arguments[i].style.background = "center transparent url('http://files.kurento.org/imgs/spinner.gif') no-repeat";
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
