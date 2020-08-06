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

var chanId = 0;

function getChannelName() {
  return "TestChannel" + chanId++;
}

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

function setIceCandidateCallbacks(webRtcPeer, webRtcEp, onerror)
{
  webRtcPeer.on('icecandidate', function(candidate) {
    console.log("Local candidate:",candidate);

    candidate = kurentoClient.getComplexType('IceCandidate')(candidate);

    webRtcEp.addIceCandidate(candidate, onerror)
  });

  webRtcEp.on('IceCandidateFound', function(event) {
    var candidate = event.candidate;

    console.log("Remote candidate:",candidate);

    webRtcPeer.addIceCandidate(candidate, onerror);
  });
}


window.addEventListener('load', function()
{
  console = new Console('console', console);

  $('#send').attr('disabled', true);
  $('#close').attr('disabled', true);
  $('#open').attr('disabled', true);

  var webRtcPeer;
  var pipeline;

  var videoInput = document.getElementById('videoInput');
  var videoOutput = document.getElementById('videoOutput');

  var startButton = document.getElementById('start');
  var stopButton = document.getElementById('stop');

  //DataChannel stuff bellow this line
  //inspired in this example
  //https://github.com/webrtc/samples/blob/gh-pages/src/content/datachannel/basic/js/main.js
  //TODO
  //This is ignoring ice servers. We should patch kurento-utils-js
  //for being able to do this correctly.

  var servers = null;
  var configuration = null;
  var peerConnection = null;

  console.log("Creating channel");
  var dataConstraints = null;
  var channel;

  function onSendChannelStateChange() {
    if (!channel) return;
    var readyState = channel.readyState;
    console.log("sencChannel state changed to " + readyState);
    if (readyState == 'open') {
      dataChannelSend.disabled = false;
      dataChannelSend.focus();
      $('#send').attr('disabled', false);
      $('#close').attr('disabled', false);
      $('#open').attr('disabled', true);
    } else {
      dataChannelSend.disabled = true;
      $('#send').attr('disabled', true);
      $('#close').attr('disabled', true);
      $('#open').attr('disabled', false);
    }
  }

  var sendButton = document.getElementById('send');
  var closeButton = document.getElementById('close');
  var openButton = document.getElementById('open');

  var dataChannelSend = document.getElementById('dataChannelSend');
  var dataChannelReceive = document.getElementById('dataChannelReceive');

  function onMessage(event) {
    console.log("Received data " + event["data"]);
    dataChannelReceive.value = event["data"];
  }

  sendButton.addEventListener("click", function() {
    var data = dataChannelSend.value;
    console.log("Send button pressed. Sending data " + data);
    channel.send(data);
    dataChannelSend.value = "";
  });

  openButton.addEventListener("click", function() {
    if (channel) {
      console.error("No more than 1 channel supported");
      return;
    }

    channel = peerConnection.createDataChannel(getChannelName(), dataConstraints);

    channel.onopen = onSendChannelStateChange;
    channel.onclose = onSendChannelStateChange;
    channel.onmessage = onMessage;
  });

  function closeChannels() {

    if (channel) {
      channel.close();
      dataChannelSend.disabled = true;
      $('#send').attr('disabled', true);
      $('#close').attr('disabled', true);
      $('#open').attr('disabled', false);
      channel = null;
    }
  }

  closeButton.addEventListener("click", function() {
    console.log("Close data channel");
    closeChannels();
  });

  //DataChannel stuff above this line

  startButton.addEventListener("click", function()
  {
    console.log("Creating external peerConnection");
    peerConnection = new RTCPeerConnection(servers, configuration);

    channel = peerConnection.createDataChannel(getChannelName(), dataConstraints);

    channel.onopen = onSendChannelStateChange;
    channel.onclose = onSendChannelStateChange;
    channel.onmessage = onMessage;

    showSpinner(videoInput, videoOutput);

    var options = {
      peerConnection: peerConnection,
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
      if (error) return onError(error)

      this.generateOffer(onOffer)
    });

    function onOffer(error, sdpOffer)
    {
      if (error) return onError(error)

      kurentoClient(args.ws_uri, function(error, kurentoClient)
      {
        if (error) return onError(error);

        kurentoClient.create("MediaPipeline", function(error, _pipeline)
        {
          if (error) return onError(error);

          pipeline = _pipeline;

          pipeline.create("WebRtcEndpoint", {useDataChannels: true}, function(error, webRtc) {
            if (error) return onError(error);

            setIceCandidateCallbacks(webRtcPeer, webRtc, onError)

            console.log("Offer: " + sdpOffer);

            webRtc.processOffer(sdpOffer, function(error, sdpAnswer) {
              if (error) return onError(error);

              console.log("Answer: " + sdpAnswer);

              webRtc.gatherCandidates(onError);

              webRtcPeer.processAnswer(sdpAnswer, onError);
            });

            webRtc.connect(webRtc, function(error) {
              if (error) return onError(error);

              console.log("Loopback established");
            });
          });
        });
      });
    }
  });
  stopButton.addEventListener("click", stop);

  function stop() {

    closeChannels();

    if (peerConnection) {
      peerConnection.close();
      peerConnection = null;
    }

    if (webRtcPeer) {
      webRtcPeer.dispose();
      webRtcPeer = null;
    }

    if (pipeline) {
      pipeline.release();
      pipeline = null;
    }

    hideSpinner(videoInput, videoOutput);
  }

  function onError(error) {
    if (error)
    {
      console.error(error);
      stop();
    }
  }
})


function showSpinner() {
  for(var i = 0; i < arguments.length; i++) {
    arguments[i].poster = 'img/transparent-1px.png';
    arguments[i].style.background = "center transparent url('img/spinner.gif') no-repeat";
  }
}

function hideSpinner() {
  for(var i = 0; i < arguments.length; i++) {
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
