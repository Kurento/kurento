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
    ws_uri: 'wss://' + location.hostname + ':8433/kurento',
    ice_servers: undefined
  }
});

if (args.ice_servers) {
  console.log("Use ICE servers: " + args.ice_servers);
  kurentoUtils.WebRtcPeer.prototype.server.iceServers = JSON.parse(args.ice_servers);
} else {
  console.log("Use freeice")
}

const file_uri = 'file:///tmp/recorderScreen.webm'; //file to be stored in media server


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
  var startRecordButton = document.getElementById('startRecordButton');
  var playButton = document.getElementById('startPlayButton');
  var stopPlayButton = document.getElementById('stopPlayButton');

  var videoPlayer = document.getElementById('videoPlayer');

  startRecordButton.addEventListener('click', startRecording);
  playButton.addEventListener('click', startPlaying);
  stopPlayButton.addEventListener('click', function(event){
    videoPlayer.src="";
  })
});

function startRecording() {
  console.log("onClick");
  var videoInput = document.getElementById("videoInput");
  var videoOutput = document.getElementById("videoOutput");

  var width, height;
  var resolution = document.getElementById('resolution').value
  switch(resolution)
  {
    case 'VGA':
      width = 640;
      height = 480;
    break;
    case 'HD':
      width = 1280;
      height = 720;
    break;
    case 'Full HD':
      width = 1920;
      height = 1080;
    break;

    default:
      return console.error('Unknown resolution',resolution)
  }

  var selectSource = document.getElementById('selectSource')
  var isWebcam = selectSource.value == 'Webcam'
  var constraints =
  {
    audio: isWebcam,
    video: {
      mandatory: {
        maxWidth: width,
        maxHeight: height,
        maxFrameRate : 15,
        minFrameRate: 15
      }
    }
  };

  if(!isWebcam)
  {
    constraints.video.mediaSource = 'screen'
    constraints.video.chromeMediaSource = 'screen'
  }

  var options = {
    localVideo: videoInput,
    remoteVideo: videoOutput,
    mediaConstraints: constraints
  };

  webRtcPeer = kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options, function(error)
  {
    if(error) return onError(error)

    this.generateOffer(onOffer)
  });

  function onOffer(error, offer) {
    if (error) return onError(error);

    console.log("Offer ...");

    kurentoClient(args.ws_uri, function(error, client) {
      if (error) return onError(error);

      client.create('MediaPipeline', function(error, pipeline) {
        if (error) return onError(error);
        console.log("Got MediaPipeline");

        pipeline.create('WebRtcEndpoint', function(error, webRtc) {
          if (error) return onError(error);
          console.log("Got WebRtcEndpoint");

          setIceCandidateCallbacks(webRtcPeer, webRtc, onError)

          webRtc.processOffer(offer, function(error, answer) {
            if (error) return onError(error);
            console.log("offer");

            webRtc.gatherCandidates(onError);
            webRtcPeer.processAnswer(answer);
          });

          var options = {uri : file_uri}

          pipeline.create('RecorderEndpoint', options, function(error, recorder) {
            if (error) return onError(error);
            console.log("Got RecorderEndpoint");

            client.connect(webRtc, webRtc, recorder, function(error) {
              if (error) return onError(error);
              console.log("Connected");
            });

            recorder.record(function(error) {
              if (error) return onError(error);
              console.log("recording");
            });

            var stopRecorderButton = document.getElementById("stopRecordButton");

            function stopRecording(event){
              recorder.stop();
              pipeline.release();
              webRtcPeer.dispose();

              videoInput.src = "";
              videoOutput.src = "";

              this.removeEventListener('click', stopRecording);
            }

            stopRecorderButton.addEventListener("click", stopRecording);
          });
        });
      });
    });
  }
}


function startPlaying() {
  kurentoClient(args.ws_uri, function(error, client) {
    if (error) return onError(error);

    var videoPlayer = document.getElementById('videoPlayer');

    client.create('MediaPipeline', function(error, pipeline) {
      if (error) return onError(error);

      function release(event)  {
        pipeline.release();
        videoPlayer.src = '';
      }

      var options = {uri : file_uri}

      pipeline.create('PlayerEndpoint', options, function(error, playerEndpoint) {
        if(error) return onError(error);

        playerEndpoint.on('EndOfStream', release);

        pipeline.create('HttpGetEndpoint', function(error, httpGetEndpoint) {
          if(error) return onError(error);

          httpGetEndpoint.getUrl(function(error, url) {
            if(error) return onError(error);
            videoPlayer.src = url;
          });

          playerEndpoint.connect(httpGetEndpoint, function(error) {
            if(error) return onError(error);

            playerEndpoint.play(function(error) {
              if(error) return onError(error);

              console.log('Playing ...');
            });
          });
        });
      });
    });
  });
}

function onError(error) {
  if(error) console.log(error);
}
