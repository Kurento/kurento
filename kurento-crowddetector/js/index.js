/*
* (C) Copyright 2014-2015 Kurento (http://kurento.org/)
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

var pipeline;
var webRtcPeer

function getopts(args, opts)
{
  var result = opts.default || {};
  args.replace(
      new RegExp("([^?=&]+)(=([^&]*))?", "g"),
      function($0, $1, $2, $3) { result[$1] = decodeURI($3); });

  return result;
};

var args = getopts(location.search,
{
  default:
  {
    // Non-secure WebSocket
    // Only valid for localhost access! Browsers won't allow using this for
    // URLs that are not localhost. Also, this matches the default KMS config:
    ws_uri: "ws://" + location.hostname + ":8888/kurento",

    // Secure WebSocket
    // Valid for localhost and remote access. To use this, you have to edit the
    // KMS settings file "kurento.conf.json", and configure the section
    // "mediaServer.net.websocket.secure". Check the docs:
    // https://doc-kurento.readthedocs.io/en/latest/features/security.html#features-security-kms-wss
    //ws_uri: "wss://" + location.hostname + ":8433/kurento",

    // Local Kurento Media Server
    // If KMS is running in localhost, use this to let it access the overlay image:
    logo_uri: location.protocol + '//127.0.0.1:' + location.port + '/img/kurento-logo.png',

    // Remote Kurento Media Server (including Docker)
    // If KMS is *not* running in localhost, provide it with the correct IP address:
    // logo_uri: location.protocol + '//172.17.0.1:' + location.port + '/img/kurento-logo.png',

    ice_servers: undefined
  }
});


  function setIceCandidateCallbacks(webRtcPeer, webRtcEp, onerror)
  {
    webRtcPeer.on('icecandidate', function(candidate) {
      console.log("Local candidate:",candidate);

      candidate = kurentoClient.getComplexType('IceCandidate')(candidate);

      webRtcEp.addIceCandidate(candidate, onerror)
    });

    webRtcEp.on('OnIceCandidate', function(event) {
      var candidate = event.candidate;

      console.log("Remote candidate:",candidate);

      webRtcPeer.addIceCandidate(candidate, onerror);
    });
  }


window.addEventListener("load", function(event)
{
  console = new Console();

  kurentoClient.register('kurento-module-crowddetector')
  const RegionOfInterest       = kurentoClient.getComplexType('crowddetector.RegionOfInterest')
  const RegionOfInterestConfig = kurentoClient.getComplexType('crowddetector.RegionOfInterestConfig')
  const RelativePoint          = kurentoClient.getComplexType('crowddetector.RelativePoint')

  var videoInput = document.getElementById('videoInput');
  var videoOutput = document.getElementById('videoOutput');

  var startButton = document.getElementById("start");
  var stopButton = document.getElementById("stop");

  stopButton.addEventListener("click", stop);

  startButton.addEventListener("click", function start()
  {
    console.log("WebRTC loopback starting");

    showSpinner(videoInput, videoOutput);

    var options =
    {
      localVideo: videoInput,
      remoteVideo: videoOutput
    }

    if (args.ice_servers) {
      console.log("Use ICE servers: " + args.ice_servers);
      options.configuration = {
        iceServers : JSON.parse(args.ice_servers)
      };
    } else {
      console.log("Use freeice")
    }

    webRtcPeer = kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options, function(error)
    {
      if(error) return onError(error)

      this.generateOffer(onOffer)
    });
  });

  function onOffer(error, sdpOffer) {
    if (error) return onError(error);

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

          setIceCandidateCallbacks(webRtcPeer, webRtc, onError)

          webRtc.processOffer(sdpOffer, function(error, sdpAnswer) {
            if (error) return onError(error);

            console.log("SDP answer obtained. Processing ...");

            webRtc.gatherCandidates(onError);

            webRtcPeer.processAnswer(sdpAnswer);
          });

          var options =
          {
            rois:
            [
              RegionOfInterest({
                id: 'roi1',
                points:
                [
                  RelativePoint({x: 0,   y: 0}),
                  RelativePoint({x: 0.5, y: 0}),
                  RelativePoint({x: 0.5, y: 0.5}),
                  RelativePoint({x: 0,   y: 0.5})
                ],
                regionOfInterestConfig: RegionOfInterestConfig({
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
                })
              })
            ]
          }

          pipeline.create('crowddetector.CrowdDetectorFilter', options, function(error, filter)
          {
            if (error) return onError(error);

            console.log("Connecting...");

            filter.on('CrowdDetectorDirection', function (data){
              console.log("Direction event received in roi " + data.roiID +
                 " with direction " + data.directionAngle);
            });

            filter.on('CrowdDetectorFluidity', function (data){
              console.log("Fluidity event received in roi " + data.roiID +
               ". Fluidity level " + data.fluidityPercentage +
               " and fluidity percentage " + data.fluidityLevel);
            });

            filter.on('CrowdDetectorOccupancy', function (data){
              console.log("Occupancy event received in roi " + data.roiID +
               ". Occupancy level " + data.occupancyPercentage +
               " and occupancy percentage " + data.occupancyLevel);
            });

            client.connect(webRtc, filter, function(error){
              if (error) return onError(error);
              console.log("WebRtcEndpoint --> Filter");

              client.connect(filter, webRtc, function(error){
                if (error) return onError(error);
                console.log("Filter --> WebRtcEndpoint");
              });
            });
          });
        });
      });
    });
  }
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
  if(error)
  {
    console.error(error);
    stop();
  }
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
