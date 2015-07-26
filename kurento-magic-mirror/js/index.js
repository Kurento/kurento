/*
* (C) Copyright 2014-2015 Kurento (http://kurento.org/)
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
      function($0, $1, $2, $3) { result[$1] = decodeURI($3); });

  return result;
};

var args = getopts(location.search,
{
  default:
  {
    ws_uri: 'ws://' + location.hostname + ':8888/kurento',
    hat_uri: 'http://' + location.host + '/img/mario-wings.png',
    ice_servers: undefined
  }
});

function setIceCandidateCallbacks(webRtcPeer, webRtcEp, onerror)
{
  webRtcPeer.on('icecandidate', function(candidate) {
    console.log("Local candidate:",candidate);

    candidate = kurentoClient.register.complexTypes.IceCandidate(candidate);

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
