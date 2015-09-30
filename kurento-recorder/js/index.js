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
    file_uri: 'file:///tmp/recorder_demo.webm', // file to be stored in media server
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


window.addEventListener('load', function(event) {
  console = new Console()

  var startRecordButton = document.getElementById('start');
  startRecordButton.addEventListener('click', startRecording);

});

function startRecording() {
  console.log("onClick");

  var videoInput = document.getElementById("videoInput");
  var videoOutput = document.getElementById("videoOutput");

  showSpinner(videoInput, videoOutput);

  var stopRecordButton = document.getElementById("stop")

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

  function onOffer(error, offer) {
    if (error) return onError(error);

    console.log("Offer...");

    kurentoClient(args.ws_uri, function(error, client) {
      if (error) return onError(error);

      client.create('MediaPipeline', function(error, pipeline) {
        if (error) return onError(error);

        console.log("Got MediaPipeline");

        var elements =
        [
          {type: 'RecorderEndpoint', params: {uri : args.file_uri}},
          {type: 'WebRtcEndpoint', params: {}}
        ]

        pipeline.create(elements, function(error, elements){
          if (error) return onError(error);

          var recorder = elements[0]
          var webRtc   = elements[1]

          setIceCandidateCallbacks(webRtcPeer, webRtc, onError)

          webRtc.processOffer(offer, function(error, answer) {
            if (error) return onError(error);

            console.log("offer");

            webRtc.gatherCandidates(onError);
            webRtcPeer.processAnswer(answer);
          });

          client.connect(webRtc, webRtc, recorder, function(error) {
            if (error) return onError(error);

            console.log("Connected");

            recorder.record(function(error) {
              if (error) return onError(error);

              console.log("record");

              stopRecordButton.addEventListener("click", function(event){
                recorder.stop();
                pipeline.release();
                webRtcPeer.dispose();
                videoInput.src = "";
                videoOutput.src = "";

                hideSpinner(videoInput, videoOutput);

                var playButton = document.getElementById('play');
                playButton.addEventListener('click', startPlaying);
              })
            });
          });
        });
      });
    });
  }
}


function startPlaying()
{
  console.log("Start playing");

  var videoPlayer = document.getElementById('videoOutput');
  showSpinner(videoPlayer);

  var options = {
    remoteVideo: videoPlayer
  };

  if (args.ice_servers) {
    console.log("Use ICE servers: " + args.ice_servers);
    options.configuration = {
      iceServers : JSON.parse(args.ice_servers)
    };
  } else {
    console.log("Use freeice")
  }

  var webRtcPeer = kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options,
  function(error)
  {
    if(error) return onError(error)

    this.generateOffer(onPlayOffer)
  });

  function onPlayOffer(error, offer) {
    if (error) return onError(error);

    kurentoClient(args.ws_uri, function(error, client) {
      if (error) return onError(error);

      client.create('MediaPipeline', function(error, pipeline) {
        if (error) return onError(error);

        pipeline.create('WebRtcEndpoint', function(error, webRtc) {
          if (error) return onError(error);

          setIceCandidateCallbacks(webRtcPeer, webRtc, onError)

          webRtc.processOffer(offer, function(error, answer) {
            if (error) return onError(error);

            webRtc.gatherCandidates(onError);

            webRtcPeer.processAnswer(answer);
          });

          var options = {uri : args.file_uri}

          pipeline.create("PlayerEndpoint", options, function(error, player) {
            if (error) return onError(error);

            player.on('EndOfStream', function(event){
              pipeline.release();
              videoPlayer.src = "";

              hideSpinner(videoPlayer);
            });

            player.connect(webRtc, function(error) {
              if (error) return onError(error);

              player.play(function(error) {
                if (error) return onError(error);
                console.log("Playing ...");
              });
            });

            document.getElementById("stop").addEventListener("click",
            function(event){
              pipeline.release();
              webRtcPeer.dispose();
              videoPlayer.src="";

              hideSpinner(videoPlayer);

            })
          });
        });
      });
    });
  };
}

function onError(error) {
  if(error) console.log(error);
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
