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

    file_uri: 'file:///tmp/kurento-hello-world-recording.webm',
    ice_servers: undefined
  }
});

var videoInput;
var videoOutput;
var webRtcPeer;
var client;
var pipeline;

const IDLE = 0;
const DISABLED = 1;
const CALLING = 2;
const PLAYING = 3;

function setStatus(nextState){
  switch(nextState){
    case IDLE:
      $('#start').attr('disabled', false)
      $('#stop').attr('disabled',  true)
      $('#play').attr('disabled',  false)
      break;

    case CALLING:
      $('#start').attr('disabled', true)
      $('#stop').attr('disabled',  false)
      $('#play').attr('disabled',  true)
      break;

    case PLAYING:
      $('#start').attr('disabled', true)
      $('#stop').attr('disabled',  false)
      $('#play').attr('disabled',  true)
      break;

    case DISABLED:
      $('#start').attr('disabled', true)
      $('#stop').attr('disabled',  true)
      $('#play').attr('disabled',  true)
      break;
  }
}


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


window.onload = function() {
  console = new Console();

  videoInput = document.getElementById('videoInput');
  videoOutput = document.getElementById('videoOutput');

  setStatus(IDLE);
}

function start() {
  setStatus(DISABLED);
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

    this.generateOffer(onStartOffer)
  });
}

function stop() {
  if (webRtcPeer) {
    webRtcPeer.dispose();
    webRtcPeer = null;
  }

  if(pipeline){
    pipeline.release();
    pipeline = null;
  }

  hideSpinner(videoInput, videoOutput);
  setStatus(IDLE);
}

function play(){
  setStatus(DISABLED)
  showSpinner(videoOutput);

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

  webRtcPeer = kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options, function(error)
  {
    if(error) return onError(error)

    this.generateOffer(onPlayOffer)
  });
}

function onPlayOffer(error, sdpOffer){
  if(error) return onError(error);

  co(function*(){
    try{
      if(!client) client = yield kurentoClient(args.ws_uri);

      pipeline = yield client.create('MediaPipeline');

      var webRtc = yield pipeline.create('WebRtcEndpoint');
      setIceCandidateCallbacks(webRtcPeer, webRtc, onError)

      var player = yield pipeline.create('PlayerEndpoint', {uri : args.file_uri});

      player.on('EndOfStream', stop);

      yield player.connect(webRtc);

      var sdpAnswer = yield webRtc.processOffer(sdpOffer);
      webRtc.gatherCandidates(onError);
      webRtcPeer.processAnswer(sdpAnswer);

      yield player.play()

      setStatus(PLAYING)
    }
    catch(e)
    {
      onError(e);
    }
  })();
}

function onStartOffer(error, sdpOffer)
{
  if(error) return onError(error)

  co(function*(){
    try{
      if(!client)
        client = yield kurentoClient(args.ws_uri);

      pipeline = yield client.create('MediaPipeline');

      var webRtc = yield pipeline.create('WebRtcEndpoint');
      setIceCandidateCallbacks(webRtcPeer, webRtc, onError)

      var recorder = yield pipeline.create('RecorderEndpoint', {uri: args.file_uri});

      yield webRtc.connect(recorder);
      yield webRtc.connect(webRtc);

      yield recorder.record();

      var sdpAnswer = yield webRtc.processOffer(sdpOffer);
      webRtc.gatherCandidates(onError);
      webRtcPeer.processAnswer(sdpAnswer)

      setStatus(CALLING);

    } catch(e){
      onError(e);
    }
  })();
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
