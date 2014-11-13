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

var pipeline;
var webRtcPeer

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
    logo_uri: 'http://' + location.host + '/img/kurento-logo.png'
  }
});


window.addEventListener("load", function(event)
{
	kurentoClient.register(kurentoModuleCrowddetector)	
	console = new Console('console', console);

	var videoInput = document.getElementById('videoInput');
	var videoOutput = document.getElementById('videoOutput');

	var startButton = document.getElementById("start");
	var stopButton = document.getElementById("stop");

	stopButton.addEventListener("click", stop);

	startButton.addEventListener("click", function start()
	{
		console.log("WebRTC loopback starting");

		showSpinner(videoInput, videoOutput);

		webRtcPeer = kurentoUtils.WebRtcPeer.startSendRecv(videoInput, videoOutput, onOffer, onError);

		function onOffer(sdpOffer) {
			console.log("onOffer");

			kurentoClient(args.ws_uri, function(error, client) {
				if (error) return onError(error);

				client.create('MediaPipeline', function(error, p) {
					if (error) return onError(error);

					pipeline = p;

					console.log("Got MediaPipeline");

					pipeline.create('WebRtcEndpoint', function(error, webRtc) {
						if (error) return onError(error);

						console.log("Got WebRtcEndpoint");

						var _roi =
						{
							id: 'roi1',
							points:
							[
							  {x: 0,   y: 0},
							  {x: 0.5, y: 0},
							  {x: 0.5, y: 0.5},
							  {x: 0,   y: 0.5}
							],
							regionOfInterestConfig:
							{
								occupancyLevelMin: 10,
								occupancyLevelMed: 35,
								occupancyLevelMax: 65,
								occupancyNumFramesToEvent: 5,
								fluidityLevelMin: 10,
								fluidityLevelMed: 35,
								fluidityLevelMax: 65,
								fluidityNumFramesToEvent: 5,
								sendOpticalFlowEvent: false,
								opticalFlowNumFramesToEvent: 3,
								opticalFlowNumFramesToReset: 3,
								opticalFlowAngleOffset: 0
							}
						};

						pipeline.create('CrowdDetectorFilter', {'rois' : [_roi]},
						 function(error, filter) {
							if (error) return onError(error);

							console.log("Connecting ...");

							webRtc.connect(filter, function(error) {
								if (error) return onError(error);

								console.log("WebRtcEndpoint --> filter");

								filter.connect(webRtc, function(error) {
									if (error) return onError(error);

									console.log("Filter --> WebRtcEndpoint");

									filter.on ('CrowdDetectorDirection', function (data){
										console.log ("Direction event received in roi " + data.roiID +
	 										" with direction " + data.directionAngle);
									});

									filter.on ('CrowdDetectorFluidity', function (data){
										console.log ("Fluidity event received in roi " + data.roiID +
										 ". Fluidity level " + data.fluidityPercentage +
										 " and fluidity percentage " + data.fluidityLevel);
									});

									filter.on ('CrowdDetectorOccupancy', function (data){
										console.log ("Occupancy event received in roi " + data.roiID +
										 ". Occupancy level " + data.occupancyPercentage +
										 " and occupancy percentage " + data.occupancyLevel);
									});
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

function stop(){
	if(webRtcPeer){
		webRtcPeer.dispose();
		webRtcPeer = null;
	}
	if(pipeline){
		pipeline.release();
		pipeline = null;
	}

	hideSpinner(videoInput, videoOutput);
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

/**
 * Lightbox utility (to display media pipeline image in a modal dialog)
 */
$(document).delegate('*[data-toggle="lightbox"]', 'click', function(event) {
	event.preventDefault();
	$(this).ekkoLightbox();
});
