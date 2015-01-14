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
    file_uri: 'file:///tmp/recorder_demo.webm', //file to be stored in media server
    ice_servers: undefined
  }
});

if (args.ice_servers) {
  console.log("Use ICE servers: " + args.ice_servers);
  kurentoUtils.WebRtcPeer.prototype.server.iceServers = JSON.parse(args.ice_servers);
} else {
  console.log("Use freeice")
}

window.addEventListener('load', function(event) {
  var startRecordButton = document.getElementById('startRecordButton');
  var playButton = document.getElementById('startPlayButton');

  startRecordButton.addEventListener('click', startRecording);
  playButton.addEventListener('click', startPlaying);

});

function startRecording() {
  console.log("onClick");

  var videoInput = document.getElementById("videoInput");
  var videoOutput = document.getElementById("videoOutput");

  webRtcPeer = kurentoUtils.WebRtcPeer.startSendRecv(videoInput, videoOutput,
      onOffer, onError);

  var disableButton = document.getElementById("disableButton");
  var enableButton  = document.getElementById("enableButton");

  var disableAudioButton = document.getElementById("disableAudioButton");
  var enableAudioButton  = document.getElementById("enableAudioButton");
  var disableVideoButton = document.getElementById("disableVideoButton");
  var enableVideoButton  = document.getElementById("enableVideoButton");

  disableButton.addEventListener('click', function()
  {
    webRtcPeer.enabled = false;
  })
  enableButton.addEventListener('click', function()
  {
    webRtcPeer.enabled = true;
  })

  disableAudioButton.addEventListener('click', function()
  {
    webRtcPeer.audioEnabled = false;
  })
  enableAudioButton.addEventListener('click', function()
  {
    webRtcPeer.audioEnabled = true;
  })
  disableVideoButton.addEventListener('click', function()
  {
    webRtcPeer.videoEnabled = false;
  })
  enableVideoButton.addEventListener('click', function()
  {
    webRtcPeer.videoEnabled = true;
  })

  function onOffer(offer) {
    console.log("Offer ...");

    kurentoClient(args.ws_uri, function(error, client) {
      if (error) return onError(error);

      client.create('MediaPipeline', function(error, pipeline) {
        if (error) return onError(error);

        console.log("Got MediaPipeline");

        pipeline.create('RecorderEndpoint', {uri : args.file_uri},
        function(error, recorder) {
          if (error) return onError(error);

          console.log("Got RecorderEndpoint");

          pipeline.create('WebRtcEndpoint', function(error, webRtc) {
            if (error) return onError(error);

            console.log("Got WebRtcEndpoint");

            webRtc.connect(recorder, function(error) {
              if (error) return onError(error);

              console.log("Connected");

              recorder.record(function(error) {
                if (error) return onError(error);

                console.log("record");

                webRtc.connect(webRtc, function(error) {
                  if (error) return onError(error);

                  console.log("Second connect");
                });

                webRtc.processOffer(offer, function(error, answer) {
                  if (error) return onError(error);

                  console.log("offer");

                  webRtcPeer.processSdpAnswer(answer);
                });

                document.getElementById("stopRecordButton").addEventListener("click",
                function(event){
                  recorder.stop();
                  pipeline.release();
                  webRtcPeer.dispose();
                  videoInput.src = "";
                  videoOutput.src = "";
                })
              });
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

  var videoPlayer = document.getElementById('videoPlayer');
  var webRtcPeer = kurentoUtils.WebRtcPeer.startRecvOnly(videoPlayer,
      onPlayOffer, onError);

  function onPlayOffer(offer) {
    kurentoClient(args.ws_uri, function(error, client) {
      if (error) return onError(error);

      client.create('MediaPipeline', function(error, pipeline) {
        pipeline.create('WebRtcEndpoint', function(error, webRtc) {
          webRtc.processOffer(offer, function(error, answer) {
            webRtcPeer.processSdpAnswer(answer);

            pipeline.create("PlayerEndpoint", {
              uri : args.file_uri
            }, function(error, player) {

              player.on('EndOfStream', function(event){
                pipeline.release();
                videoPlayer.src = "";
              });

              player.connect(webRtc, function(error) {
                if (error) return onError(error);

                player.play(function(error) {
                  if (error) return onError(error);
                  console.log("Playing ...");
                });
              });

              document.getElementById("stopPlayButton").addEventListener("click",
              function(event){
                pipeline.release();
                webRtcPeer.dispose();
                videoPlayer.src="";
              })
            });
          });
        });
      });
    });
  };
}

function onError(error) {
  console.log(error);
}
