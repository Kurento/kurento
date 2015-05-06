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
 */

window.onload = function() {
  console = new Console('console', console);

  var webRtcPeer;
  var rpcBuilder;

  var videoInput  = document.getElementById('videoInput');
  var videoOutput = document.getElementById('videoOutput');

  var btnStart = document.getElementById('btnStart');
  var btnStop  = document.getElementById('btnStop');

  function onRequest(request)
  {
    switch(request.method)
    {
      case 'candidate':
        webRtcPeer.addIceCandidate(request.params[0])
      break;

      default:
        console.error(request)
    }
  }

  console.log("Loading complete...");

  btnStart.addEventListener('click', function() {
    console.log("Starting video call ...");
    showSpinner(videoInput, videoOutput);

		const packer = RpcBuilder.packers.JsonRPC;

    rpcBuilder = new RpcBuilder(packer, new WebSocket('ws:'+location.host),
    onRequest);

    var options =
    {
      localVideo: videoInput,
      remoteVideo: videoOutput
    }

    webRtcPeer = kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options, function(error)
    {
      if(error) return onError(error)

      this.generateOffer(onOffer)
    });

    webRtcPeer.on('icecandidate', function(candidate) {
      rpcBuilder.encode('candidate', [candidate])
    });
  })

  btnStop.addEventListener('click', function() {
    if (webRtcPeer) {
      console.log("Stopping video call ...");
      webRtcPeer.dispose();
      webRtcPeer = null;

      rpcBuilder.close()
    }
    hideSpinner(videoInput, videoOutput);
  })

  function onOffer(error, sdpOffer) {
    if(error) return onError(error)

    console.info('Invoking SDP offer callback function ' + location.host);

    rpcBuilder.encode('offer', [sdpOffer], processAnswer)
  }

  function processAnswer(error, sdpAnswer) {
    if(error) return onError(error)

    console.log("Received sdpAnswer from server. Processing ...");
    webRtcPeer.processAnswer(sdpAnswer);
  }
}

function onError(error) {
  if(error) console.error(error);
}

function showSpinner() {
  for (var i = 0; i < arguments.length; i++) {
    arguments[i].poster = './img/transparent-1px.png';
    arguments[i].style.background = "center transparent url('./img/spinner.gif') no-repeat";
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
