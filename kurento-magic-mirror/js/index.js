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
    // Non-secure WebSocket (only for localhost tests)
    //ws_uri: 'ws://' + location.hostname + ':8888/kurento',

    // Secure WebSocket (for localhost and remote tests)
    // To use this, you have to enable the "mediaServer.net.websocket.secure"
    // KMS setting in "kurento.conf.json".
    //
    // Also, note that most browsers will reject self-signed certificates for
    // Secure WebSockets connections. You have to either use a proper
    // certificate, or install your self-signed Root CA in the browser. For this
    // last option, we recommend using the tool "mkcert".
    ws_uri: 'wss://' + location.hostname + ':8433/kurento',

    hat_uri: 'https://' + location.host + '/img/mario-wings.png',
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

  var pipeline;
  var webRtcPeer;

  var videoInput = document.getElementById('videoInput');
  var videoOutput = document.getElementById('videoOutput');

  var startButton = document.getElementById("start");
  var stopButton = document.getElementById("stop");
      stopButton.addEventListener("click", stop);


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


  startButton.addEventListener("click", function start()
  {
    console.log("WebRTC loopback starting");

    showSpinner(videoInput, videoOutput);

    var options = {
      localVideo: videoInput,
      remoteVideo: videoOutput
    };

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
    console.log("onOffer");

    if(error) return onError(error)

    kurentoClient(args.ws_uri, function(error, client) {
      if (error) return onError(error);

      client.create('MediaPipeline', function(error, _pipeline) {
        if (error) return onError(error);

        pipeline = _pipeline;

        console.log("Got MediaPipeline");

        pipeline.create('WebRtcEndpoint', function(error, webRtcEp) {
          if (error) return onError(error);

          setIceCandidateCallbacks(webRtcPeer, webRtcEp, onError)

          console.log("Got WebRtcEndpoint");

          webRtcEp.processOffer(sdpOffer, function(error, sdpAnswer) {
            if (error) return onError(error);

            console.log("SDP answer obtained. Processing...");

            webRtcPeer.processAnswer(sdpAnswer, onError);
          });
          webRtcEp.gatherCandidates(onError);

          pipeline.create('FaceOverlayFilter', function(error, filter) {
            if (error) return onError(error);

            console.log("Got FaceOverlayFilter");

            filter.setOverlayedImage(args.hat_uri, -0.35, -1.2, 1.6, 1.6,
            function(error) {
              if (error) return onError(error);

              console.log("Set overlay image");
            });

            console.log("Connecting...");

            client.connect(webRtcEp, filter, webRtcEp, function(error) {
              if (error) return onError(error);

              console.log("WebRtcEndpoint --> filter --> WebRtcEndpoint");
            });
          });
        });
      });
    });
  }
});


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
