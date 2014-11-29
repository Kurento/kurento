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

function getopts(args, opts)
{
  var result = opts.default || {};
  args.replace(
      new RegExp("([^?=&]+)(=([^&]*))?", "g"),
      function($0, $1, $2, $3) { result[$1] = $3; });

  return result;
};

var args = getopts(location.search,
{
  default:
  {
    ws_uri: 'ws://' + location.hostname + ':8888/kurento',
    as_uri: location.href,
    ice_servers: undefined
  }
});

if (args.ice_servers) {
  console.log("Use ICE servers: " + args.ice_servers);
  kurentoUtils.WebRtcPeer.prototype.server.iceServers = JSON.parse(args.ice_servers);
} else {
  console.log("Use freeice")
}


const ivan   = args.as_uri + '/img/ivan_.webm';
const raquel = args.as_uri + '/img/raquel_.webm';
const borja  = args.as_uri + '/img/borja_.webm';
const clara  = args.as_uri + '/img/clara_.webm';

const raquelImg           = args.as_uri+'/img/Raquel.png';
const raquelImgDeactivate =  args.as_uri+'/img/Raquel_deactivate.png';
const borjaImg           = args.as_uri+'/img/borja.png';
const borjaImgDeactivate = args.as_uri+'/img/borja_deactivate.png';
const ivanImg           = args.as_uri+'/img/ivan.png';
const ivanImgDeactivate = args.as_uri+'/img/ivan_deactivate.png';
const userImg           = args.as_uri+'/img/clara.png';
const userImgDeactivate = args.as_uri+'/img/clara_deactivate.png';


if(typeof kurentoClient == 'undefined')
  kurentoClient = require('kurento-client');

kurentoClient.register(kurentoModuleFacesegmentator)

var sample1, sample2, sample3, sample4;
var playButton, stopButton;
var videoOutput;
var webRtcPeer;
var rightPointX = 0;
var rightPointY = 0;
var leftPointX = 0;
var leftPointY = 0;
var pipeline;
var alphaBlending;
var playerEndpoint;
var video_port = null;
var webRtc_port;
var webRtc;
var gstreamerFilter;
var gstreamerFilterBox;
var gstreamerFilterCrop;
var gstreamerFilterFlip;
var kissing = false;
// var micaX = 318;
// var micaY = 113;
//ivan2
// var ivanX = 582;
// var ivanY = 119;
var ivanX = 553;
var ivanY = 180;
//chica 2
// var raquelX = 478;
// var raquelY = 196;
var raquelX = 560;
var raquelY = 186;
//borja 1
// var borjaX = 500;
// var borjaY = 155;
var borjaX = 579;
var borjaY = 158;
//clara
// var claraX = 524;
// var claraY = 164;
var claraX = 588;
var claraY = 155;


window.onload = function() {
	console = new Console('console', console);
	videoOutput = document.getElementById('videoOutput');
	sample1 = document.getElementById('sample1');
	sample2 = document.getElementById('sample2');
	sample3 = document.getElementById('sample3');
	sample4 = document.getElementById('sample4');
	playButton = document.getElementById ('start');
	
	$('#stop').attr('disabled', true);
	$('#start').attr('disabled', false);
}

function start() {
	showSpinner(videoOutput);
	webRtcPeer = kurentoUtils.WebRtcPeer.startSendRecv(null, videoOutput, onOffer, onError);
	
	$('#stop').attr('disabled', false);
	$('#start').attr('disabled', true);
}

function stop() {
	if (webRtcPeer) {
		webRtcPeer.dispose();
		webRtcPeer = null;
	}
	if(pipeline){
		pipeline.release();
		pipeline = null;
	}
	hideSpinner(videoOutput);
	hideImages (sample1, sample2, sample3, sample4);

	$('#stop').attr('disabled', true);
	$('#start').attr('disabled', false);

	kissing = false;
}

function onOffer(sdpOffer) {
    kurentoClient(args.ws_uri, function(error, kurentoClient) {
		if (error) return onError(error);

		kurentoClient.create('MediaPipeline', function(error, _pipeline) {
		    if (error) return onError(error);

		    pipeline = _pipeline;
		    pipeline.create('WebRtcEndpoint', function(error, _webRtc) {
				if (error) return onError(error);

				webRtc = _webRtc;
				pipeline.create('GStreamerFilter', {command : 'videoflip method=4'}, function(error, _gstreamerFilterFlip) {
					if(error) return onError(error);
					
					gstreamerFilterFlip = _gstreamerFilterFlip;
				    pipeline.create('AlphaBlending', function(error, _alphaBlending) {
						if(error) return onError(error);

					    alphaBlending = _alphaBlending;
					    alphaBlending.createHubPort (function(error, _webRtc_port) {
							if (error) return onError(error);

							webRtc_port = _webRtc_port;
							pipeline.create ('FaceSegmentatorFilter', function(error, facesegmentator) {
							    if (error) return onError(error);

							    webRtc.connect(gstreamerFilterFlip, function(error) {
										if (error) return onError(error);

							        gstreamerFilterFlip.connect(facesegmentator, function(error) {
										if (error) return onError(error);

										facesegmentator.connect(webRtc_port, function(error) {
										    if (error) return onError(error);

											webRtc_port.connect(webRtc, function(error) {
											    if (error) return onError(error);											

												alphaBlending.setMaster (webRtc_port, 3, function(error) {
													if (error) return onError(error);
														console.log("Set Master Port");
											    });

											    facesegmentator.on('FacePosition', function(data)
											    {
											      rightPointX = data.rightPoint.x;
											      rightPointY = data.rightPoint.y;
											      leftPointX = data.leftPoint.x;
											      leftPointY = data.leftPoint.y;
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
		});
    });
	showImages (sample1, raquelImg, sample2, borjaImg, sample3, ivanImg, sample4, userImg);
}

function kiss( video, xPos, yPos) {
	if ((leftPointX == 0) && (leftPointX == 0) && (rightPointX == 0) && (rightPointY == 0)) {
		console.log ("There are not face points");
		kissing = false;
		reactivateImages (sample1, raquelImg, sample1click, 
			sample2, borjaImg, sample2click,
			sample3, ivanImg, sample3click, 
			sample4, userImg, sample4click);
		return;
	}

	//calculate union point
	var unionX = (leftPointX - xPos);
	var unionY = (leftPointY - yPos);

	var moveX = "";
	var moveY = "";

	var cropX = "";
	var cropY = "";

	console.log ("L x point " + leftPointX + " ypoint " + leftPointY);
	console.log ("R x point " + rightPointX + " ypoint " + rightPointY)
	
	if (unionX < 0){
		moveX  = " right=" + unionX;
		cropX  = " left=" + ((-1) * unionX);
	} else {
		moveX  = " left=" + ((-1) * unionX);
		cropX  = " right=" + unionX;
	}

	if (unionY < 0){
		moveY  = " bottom=" + unionY;
		cropY  = " top=" + ((-1) * unionY);
	} else {
		moveY  = " top=" + ((-1) * unionY);
		cropY  = " bottom=" + unionY;
	}

    pipeline.create('PlayerEndpoint', {uri : video}, function(error, _playerEndpoint) {
		if(error) return onError(error);

		playerEndpoint = _playerEndpoint;
		//pipeline.create ('GStreamerFilter', {command : 'alpha method=custom target-r=70 target-g=140 target-b=140 angle=25'}, function (error, _gstreamerFilter) {
			pipeline.create ('GStreamerFilter', {command : 'alpha method=custom target-r=124 target-g=167 target-b=154 angle=30'}, function (error, _gstreamerFilter) {
		    if(error) return onError(error);

		    gstreamerFilter = _gstreamerFilter;
		    var _command = "videobox border-alpha=0" + moveX + moveY;
			pipeline.create ('GStreamerFilter', {command : _command}, function (error, _gstreamerFilterBox) {
		    	if(error) return onError(error);

		    	gstreamerFilterBox = _gstreamerFilterBox;
		    	_command = "videocrop" + cropX + cropY;
				pipeline.create ('GStreamerFilter', {command : _command}, function (error, _gstreamerFilterCrop) {
			    	if(error) return onError(error);

		    		gstreamerFilterCrop = _gstreamerFilterCrop;	
				    alphaBlending.createHubPort (function(error, _video_port) {
						if (error) return onError(error);

						video_port = _video_port;
						playerEndpoint.connect(gstreamerFilter, function(error) {
						    if (error) return onError(error);
							
							gstreamerFilter.connect(gstreamerFilterBox, function(error) {
						    	if (error) return onError(error);

						    	gstreamerFilterBox.connect(gstreamerFilterCrop, function(error) {
						    	if (error) return onError(error);

									gstreamerFilterCrop.connect(video_port, function(error) {
										if (error) return onError(error);

										alphaBlending.setPortProperties (0, 0, 4, 1, 1, video_port, function(error) {
										 	if (error) return onError(error);
										 		console.log("Setting port properties");
									    });

										playerEndpoint.play(function(error){
										    if(error) return onError(error);

										    console.log('Playing ...');

											playerEndpoint.on('EndOfStream', function(data)
										    {
									    		video_port.release ();
												video_port = null;
												playerEndpoint.release ();
												gstreamerFilter.release();
												gstreamerFilterBox.release();
												gstreamerFilterCrop.release();
												kissing = false;
												reactivateImages (sample1, raquelImg, sample1click, 
													sample2, borjaImg, sample2click,
													sample3, ivanImg, sample3click, 
													sample4, userImg, sample4click);
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
    });
}

function sample1click() {
	if (kissing == false ) { 
		kissing = true;
		deactivateImages (sample1, raquelImgDeactivate, sample2, borjaImgDeactivate, 
			sample3, ivanImgDeactivate, sample4, userImgDeactivate);
		kiss (raquel, raquelX, raquelY);
	}
}

function sample2click() {
	if (kissing == false ) { 
		kissing = true;
		deactivateImages (sample1, raquelImgDeactivate, sample2, borjaImgDeactivate, 
			sample3, ivanImgDeactivate, sample4, userImgDeactivate);
		kiss (borja, borjaX, borjaY);
	}
	
}

function sample3click() {
	if (kissing == false ) { 
		kissing = true;
		deactivateImages (sample1, raquelImgDeactivate, sample2, borjaImgDeactivate, 
			sample3, ivanImgDeactivate, sample4, userImgDeactivate);
		kiss (ivan, ivanX, ivanY);
	}
}

function sample4click() {
	if (kissing == false ) { 
		kissing = true;
		deactivateImages (sample1, raquelImgDeactivate, sample2, borjaImgDeactivate, 
			sample3, ivanImgDeactivate, sample4, userImgDeactivate);
		kiss (clara, claraX, claraY);
	}
}

function onError(error) {
	if(error) console.error(error);
	stop();
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

function showImages() {
	for (var i = 0; i < arguments.length; i=i+2) {
		arguments[i].poster = arguments[i+1];
	}
}

function hideImages() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].poster = args.as_uri+'/img/user.png';
	}
}

function deactivateImages() {
	for (var i = 0; i < arguments.length; i=i+2) {
		arguments[i].poster = arguments[i+1];
		arguments[i].style.cursor = "default";
		arguments[i].onclick = '';
	}
}

function reactivateImages() {
	for (var i = 0; i < arguments.length; i=i+3) {
		arguments[i].poster = arguments[i+1];
		arguments[i].style.cursor = "pointer";
		arguments[i].onclick = arguments[i+2];
	}
}

/**
 * Lightbox utility (to display media pipeline image in a modal dialog)
 */
$(document).delegate('*[data-toggle="lightbox"]', 'click', function(event) {
	event.preventDefault();
	$(this).ekkoLightbox();
});
