/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

var ws = new WebSocket('wss://' + location.host + '/crowddetector');
var videoOutput;
var webRtcPeer;
var state = null;

var feed = null;
var roisLoaded = false;
var created = false;

var conn;
var roiscounter = 0;

var roisValues;

const I_CAN_START = 0;
const I_CAN_STOP = 1;
const I_AM_STARTING = 2;

window.onload = function() {
	console.log("Page loaded ...");
	console = new Console('console', console);
	videoOutput = document.getElementById('videoOutput');
	setState(I_CAN_START);
	
	init();
}

window.onbeforeunload = function() {
	stop();
	ws.close();
}

ws.onmessage = function(message) {
	var parsedMessage = JSON.parse(message.data);
	console.info('Received message: ' + message.data);

	switch (parsedMessage.id) {
	case 'startResponse':
		startResponse(parsedMessage);
		break;
	case 'noPlayer':
		noPlayer();
		break;
	case 'noPlaying':
		noPlaying();
		break;
	case 'error':
		if (state == I_AM_STARTING) {
			setState(I_CAN_START);
		}
		onError("Error message from server: " + parsedMessage.message);
		break;
	case 'iceCandidate':
	    webRtcPeer.addIceCandidate(parsedMessage.candidate, function (error) {
	        if (error) {
		      console.error("Error adding candidate: " + error);
		      return;
	        }
	    });
	    break;
	default:
		if (state == I_AM_STARTING) {
			setState(I_CAN_START);
		}
		onError('Unrecognized message', parsedMessage);
	}
}

function noPlayer () {
	alert ("Player not configured. Please set a feed to playing");
	setState(I_CAN_START);
	hideSpinner(videoOutput);
    document.getElementById('changeFeed').onclick= changeFeed;
    document.getElementById('address').disabled=false;    
}

function start() {
	console.log("Starting video call ...")
	// Disable start button
	setState(I_AM_STARTING);
	showSpinner(videoOutput);

	console.log("Creating WebRtcPeer and generating local sdp offer ...");

	var options = {
		      remoteVideo: videoOutput,
		      onicecandidate: onIceCandidate
    }
	webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options,
		function (error) {
		  if(error) {
			  return console.error(error);
		  }
		  this.generateOffer (onOffer);
	});
	created = true;
}

function onOffer(error, offerSdp) {
	if (error) return console.error (error);
	console.info('Invoking SDP offer callback function ' + location.host);

	if (feed == null){
		var message = {
			id : 'start',
			sdpOffer : offerSdp
		}
	} else {
		var message = {
			id : 'start',
			sdpOffer : offerSdp,
			feedUrl : feed
		}
	}
	sendMessage(message);	
}

function onIceCandidate(candidate) {
	  console.log("Local candidate" + JSON.stringify(candidate));

	  var message = {
	    id: 'onIceCandidate',
	    candidate: candidate
	  };
	  sendMessage(message);
}

function onError(error) {
	roisValues = new Array();
	setState(I_CAN_PLAY);
	console.error(error);
}

function startResponse(message) {
	setState(I_CAN_STOP);
	console.log("SDP answer received from server. Processing ...");
	webRtcPeer.processAnswer (message.sdpAnswer, function (error) {
		if (error) return console.error (error);
	});
	
    document.getElementById('changeFeed').onclick= changeFeed;
    document.getElementById('address').disabled=false;
    document.getElementById('address').value=message.feedUrl;
    
    if (!roisLoaded) {
    	roisValues = JSON.parse(message.rois);
        
	    select = document.getElementById('rois');
	    
	    for (var i = 0, len = roisValues.length; i < len; ++i) {
	        var opt = document.createElement('option');
	        opt.value = roisValues[i].id;
	        opt.innerHTML = roisValues[i].id;
	        select.appendChild(opt);
	    }
	    changeRoi();
	    roisLoaded = true;
    }
}

function changeRoi() {
	var selectValue = document.getElementById('rois').value;
    for (var i = 0, len = roisValues.length; i < len; ++i) {
        if (roisValues[i].id == selectValue){
			document.getElementById('rangeValue1').value = roisValues[i].regionOfInterestConfig.occupancyLevelMin;
			document.getElementById('rangeValue2').value = roisValues[i].regionOfInterestConfig.occupancyLevelMed;
			document.getElementById('rangeValue3').value = roisValues[i].regionOfInterestConfig.occupancyLevelMax;
			document.getElementById('rangeValue4').value = roisValues[i].regionOfInterestConfig.occupancyNumFramesToEvent;
			document.getElementById('rangeValue5').value = roisValues[i].regionOfInterestConfig.fluidityLevelMin;
			document.getElementById('rangeValue6').value = roisValues[i].regionOfInterestConfig.fluidityLevelMed;
			document.getElementById('rangeValue7').value = roisValues[i].regionOfInterestConfig.fluidityLevelMax;
			document.getElementById('rangeValue8').value = roisValues[i].regionOfInterestConfig.fluidityNumFramesToEvent;
			document.getElementById('rangeValue9').value = roisValues[i].regionOfInterestConfig.opticalFlowNumFramesToEvent;
			document.getElementById('rangeValue10').value = roisValues[i].regionOfInterestConfig.opticalFlowNumFramesToReset;
			document.getElementById('rangeValue11').value = roisValues[i].regionOfInterestConfig.opticalFlowAngleOffset;
			
			if (roisValues[i].regionOfInterestConfig.sendOpticalFlowEvent == true) {
				document.getElementById('true').value = true;
				document.getElementById('false').value = false;
			} else {
				document.getElementById('true').value = false;
				document.getElementById('false').value = true;
			}
			
			document.getElementById('rangeValue1_1').value = roisValues[i].regionOfInterestConfig.occupancyLevelMin;
			document.getElementById('rangeValue2_1').value = roisValues[i].regionOfInterestConfig.occupancyLevelMed;
			document.getElementById('rangeValue3_1').value = roisValues[i].regionOfInterestConfig.occupancyLevelMax;
			document.getElementById('rangeValue4_1').value = roisValues[i].regionOfInterestConfig.occupancyNumFramesToEvent;
			document.getElementById('rangeValue5_1').value = roisValues[i].regionOfInterestConfig.fluidityLevelMin;
			document.getElementById('rangeValue6_1').value = roisValues[i].regionOfInterestConfig.fluidityLevelMed;
			document.getElementById('rangeValue7_1').value = roisValues[i].regionOfInterestConfig.fluidityLevelMax;
			document.getElementById('rangeValue8_1').value = roisValues[i].regionOfInterestConfig.fluidityNumFramesToEvent;
			document.getElementById('rangeValue9_1').value = roisValues[i].regionOfInterestConfig.opticalFlowNumFramesToEvent;
			document.getElementById('rangeValue10_1').value = roisValues[i].regionOfInterestConfig.opticalFlowNumFramesToReset;
			document.getElementById('rangeValue11_1').value = roisValues[i].regionOfInterestConfig.opticalFlowAngleOffset; 	
        }        
    }
    document.getElementById('rangeValue12_1').disabled = false;
    document.getElementById('rangeValue12_1').value = 640;
    document.getElementById('rangeValue12').value = 640;

}

function stop() {
	console.log("Stopping video call ...");
	setState(I_CAN_START);
	if (webRtcPeer) {
		webRtcPeer.dispose();
		webRtcPeer = null;

		var message = {
			id : 'stop'
		}
		sendMessage(message);
	}
	hideSpinner(videoOutput);
    document.getElementById('changeFeed').onclick= changeFeed;
    document.getElementById('address').disabled=false;
    document.getElementById('address').value="";
    feed = null;
}

function setState(nextState) {
	switch (nextState) {
	case I_CAN_START:
		$('#start').attr('disabled', false);
		$('#stop').attr('disabled', true);
		break;

	case I_CAN_STOP:
		$('#start').attr('disabled', true);
		$('#stop').attr('disabled', false);
		break;

	case I_AM_STARTING:
		$('#start').attr('disabled', true);
		$('#stop').attr('disabled', true);
		break;

	default:
		onError("Unknown state " + nextState);
		return;
	}
	state = nextState;
}

function sendMessage(message) {
	var jsonMessage = JSON.stringify(message);
	console.log('Sending message: ' + jsonMessage);
	ws.send(jsonMessage);
}

function showSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].poster = './img/transparent-1px.png';
		arguments[i].style.background = "center transparent url('./img/spinner.gif') no-repeat";
	}
}

function hideSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].src = '';
		arguments[i].poster = './img/webrtc.png';
		arguments[i].style.background = '';
	}
}

function updateValue(val, name) {
	document.getElementById(name).value = val;
	changeProcessingWidth (val);
}
	
function init() {
	document.getElementById('rangeValue1').value = 0;
	document.getElementById('rangeValue2').value = 0;
	document.getElementById('rangeValue3').value = 0;
	document.getElementById('rangeValue4').value = 0;
	document.getElementById('rangeValue5').value = 0;
	document.getElementById('rangeValue6').value = 0;
	document.getElementById('rangeValue7').value = 0;
	document.getElementById('rangeValue8').value = 0;
	document.getElementById('rangeValue9').value = 0;
	document.getElementById('rangeValue10').value = 0;
	document.getElementById('rangeValue11').value = 0;
	document.getElementById('rangeValue12').value = 160;
}

function changeFeed(){
	feed = document.getElementById('address').value;
	alert ("Feed updated");
	if (!created) {
		start ();
	} else {
		var message = {
			id : 'updateFeed',
			feedUrl : feed
		}
		sendMessage(message);
	}
}

function noPlaying() {
	console.log ("Video feed not available.");
	setState(I_CAN_START);
	hideSpinner(videoOutput);
}

function changeProcessingWidth (width){
	value = document.getElementById('rangeValue12').value;
	var message = {
		id : 'changeProcessingWidth',
		width : value
	}
	sendMessage(message);
}
	
/**
 * Lightbox utility (to display media pipeline image in a modal dialog)
 */
$(document).delegate('*[data-toggle="lightbox"]', 'click', function(event) {
	event.preventDefault();
	$(this).ekkoLightbox();
});
