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
      function($0, $1, $2, $3) { result[$1] = $3; });

  return result;
};

var args = getopts(location.search,
{
  default:
  {
    ws_uri: 'ws://' + location.hostname + ':8888/kurento',
    logo_uri: 'http://' + location.host + '/img/kurento-logo.png',
    ice_servers: undefined
  }
});

if (args.ice_servers) {
  console.log("Use ICE servers: " + args.ice_servers);
  kurentoUtils.WebRtcPeer.prototype.server.iceServers = JSON.parse(args.ice_servers);
} else {
  console.log("Use freeice")
}

kurentoClient.register('kurento-module-markerdetector')


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


window.addEventListener('load', function()
{
  console = new Console('console', console);

  var webRtcPeer;
  var pipeline;

  var videoInput = document.getElementById('videoInput');
  var videoOutput = document.getElementById('videoOutput');

  var startButton = document.getElementById("start");
  var stopButton = document.getElementById("stop");

  startButton.addEventListener("click", function()
  {
    showSpinner(videoInput, videoOutput);

    var options = {
      localVideo: videoInput,
      remoteVideo: videoOutput
    };

    webRtcPeer = kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options, function(error)
    {
      if(error) return onError(error)

      this.generateOffer(onOffer)
    });

    function onOffer(error, sdpOffer)
    {
      if(error) return onError(error)

      kurentoClient(args.ws_uri, function(error, client)
      {
        if (error) return onError(error);

        client.create('MediaPipeline', function(error, _pipeline)
        {
          if (error) return onError(error);

          pipeline = _pipeline;

          pipeline.create('WebRtcEndpoint', function(error, webRtc)
          {
            if (error) return onError(error);

            setIceCandidateCallbacks(webRtcPeer, webRtc, onError)

            webRtc.processOffer(sdpOffer, function(error, sdpAnswer)
            {
              if (error) return onError(error);

              console.log("SDP answer obtained. Processing...");

              webRtc.gatherCandidates(onError);

              webRtcPeer.processAnswer(sdpAnswer);
            });

            pipeline.create('ArMarkerdetector', function(error, filter)
            {
              if (error) return onError(error);

              filter.setOverlayImage(args.logo_uri, function(error)
              {
                if (error) return onError(error);

                console.log("Set Image");
              });

              client.connect(webRtc, filter, webRtc, function(error)
              {
                if (error) return onError(error);

                console.log("WebRtcEndpoint --> filter --> WebRtcEndpoint");
              });
            });
          });
        });
      });
    }

    $('#stop').attr('disabled', false);
    $('#start').attr('disabled', true);
  })
  stopButton.addEventListener("click", stop);

  function stop() {
    if(pipeline){
      pipeline.release();
      pipeline = null;
    }
    if (webRtcPeer) {
      webRtcPeer.dispose();
      webRtcPeer = null;
    }

    hideSpinner(videoInput, videoOutput);

    $('#stop').attr('disabled', true);
    $('#start').attr('disabled', false);
  }

  function onError(error)
  {
    if(error)
    {
      console.error(error);
      stop();
    }
  }

  $('#stop').attr('disabled', true);
  $('#start').attr('disabled', false);
})


function showSpinner() {
  for (var i = 0; i < arguments.length; i++) {
    arguments[i].poster = 'img/transparent-1px.png';
    arguments[i].style.background = "center transparent url('img/spinner.gif') no-repeat";
  }
}

function hideSpinner() {
  for (var i = 0; i < arguments.length; i++) {
    arguments[i].src = '';
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
