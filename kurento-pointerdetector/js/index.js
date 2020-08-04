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

    ice_servers: undefined
  }
});

var filter = null;
var pipeline;
var webRtcPeer


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
  kurentoClient.register('kurento-module-pointerdetector')

  const PointerDetectorWindowMediaParam = kurentoClient.getComplexType('pointerdetector.PointerDetectorWindowMediaParam')
  const WindowParam                     = kurentoClient.getComplexType('pointerdetector.WindowParam')

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

      client.create('MediaPipeline', function(error, _pipeline) {
        if (error) return onError(error);

        pipeline = _pipeline;

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
            calibrationRegion: WindowParam({
              topRightCornerX: 5,
              topRightCornerY:5,
              width:30,
              height: 30
            })
          };

          pipeline.create('pointerdetector.PointerDetectorFilter', options, function(error, _filter) {
            if (error) return onError(error);

            filter = _filter;

            var options = PointerDetectorWindowMediaParam({
              id: 'window0',
              height: 50,
              width:50,
              upperRightX: 500,
              upperRightY: 150
            });

            filter.addWindow(options, onError);

            var options = PointerDetectorWindowMediaParam({
              id: 'window1',
              height: 50,
              width:50,
              upperRightX: 500,
              upperRightY: 250
            });

            filter.addWindow(options, onError);

            filter.on ('WindowIn', function (data){
              console.log ("Event window in detected in window " + data.windowId);
            });

            filter.on ('WindowOut', function (data){
              console.log ("Event window out detected in window " + data.windowId);
            });

            console.log("Connecting ...");

            client.connect(webRtc, filter, function(error) {
              if (error) return onError(error);
              console.log("WebRtcEndpoint --> Filter");

              client.connect(filter, webRtc, function(error) {
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

function calibrate() {
  if(filter) filter.trackColorFromCalibrationRegion(onError);
}

function onError(error) {
  if(error) console.error(error);
}

function stop(){
  if(pipeline){
    pipeline.release();
    pipeline = null;
  }

  if(webRtcPeer){
    webRtcPeer.dispose();
    webRtcPeer = null;
  }

  hideSpinner(videoInput, videoOutput);
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
