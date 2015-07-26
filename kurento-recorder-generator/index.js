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
    ws_uri:   'ws://' + location.hostname + ':8888/kurento',
    file_uri: 'file:///tmp/recorder_demo.webm', //file to be stored in media server
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

  var startRecordButton = document.getElementById('startRecordButton');
  var playButton = document.getElementById('startPlayButton');

  startRecordButton.addEventListener('click', startRecording);
  playButton.addEventListener('click', startPlaying);
});

function startRecording() {
  var videoInput = document.getElementById("videoInput");
  var videoOutput = document.getElementById("videoOutput");
  var stopRecordButton = document.getElementById('stopRecordButton');

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
    if(error) return onError(error)

    console.log("Offer ...");
    co(function*(){
      try{
        var client = yield kurentoClient(args.ws_uri);
        pipeline   = yield client.create("MediaPipeline");

        var webRtc = yield pipeline.create("WebRtcEndpoint");
        setIceCandidateCallbacks(webRtcPeer, webRtc, onError)

        var answer = yield webRtc.processOffer(offer);
        webRtc.gatherCandidates(onError);
        webRtcPeer.processAnswer(answer);

        recorder = yield pipeline.create("RecorderEndpoint", {uri: args.file_uri});

        yield webRtc.connect(webRtc);
        yield webRtc.connect(recorder);
        yield recorder.record();

        stopRecordButton.addEventListener("click", function(event){
          co(function*(){
            yield recorder.stop(); //Need to guarantee that this executes before pipeline.release();
            yield pipeline.release();
            webRtcPeer.dispose();
            videoInput.src="";
            videoOutput.src="";
          })();
        });

      }catch(e){
        console.log(e);
      }
    })();
  }
}


function startPlaying() {

  console.log("Start playing");

  var videoPlayer = document.getElementById("videoPlayer");
  var stopPlayButton = document.getElementById("stopPlayButton");

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

  var webRtcPeer = kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options, function(error)
  {
    if(error) return onError(error)

    this.generateOffer(onPlayOffer)
  });

  function onPlayOffer(error, offer) {
    if(error) return onError(error)

    console.log("Offer ...")

    co(function*(){
      try{
        var client = yield kurentoClient(args.ws_uri);
        var pipeline = yield client.create("MediaPipeline");

        var webRtc = yield pipeline.create("WebRtcEndpoint");
        setIceCandidateCallbacks(webRtcPeer, webRtc, onError)

        var answer = yield webRtc.processOffer(offer);
        webRtc.gatherCandidates(onError);
        webRtcPeer.processAnswer(answer);

        var player = yield pipeline.create("PlayerEndpoint", {uri: args.file_uri});

        yield player.connect(webRtc);
        yield player.play();

        player.on("EndOfStream", function(event){
          pipeline.release();
          webRtcPeer.dispose();
          videoPlayer.src="";
        });

        stopPlayButton.addEventListener("click", function(event){
          pipeline.release();
          webRtcPeer.dispose();
          videoPlayer.src = "";
        });

      } catch(e) {
        console.log(e);
      }

    })();
  }
}

function onError(error) {
  if(error) console.log(error);
}
