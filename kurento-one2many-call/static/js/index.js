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

window.onload = function() {
  console = new Console('console', console);

  var webRtcPeer;

  const packer = RpcBuilder.packers.JsonRPC;

  var rpcBuilder = new RpcBuilder(packer, new WebSocket('ws:'+location.host),
  onRequest);

  window.onbeforeunload = rpcBuilder.close.bind(rpcBuilder);

  var video = document.getElementById('video');

  var btnCall = document.getElementById('call');
  var btnViewer = document.getElementById('viewer');
  var btnTerminate = document.getElementById('terminate');

  btnCall.addEventListener('click', function()
  {
    if (!webRtcPeer) {
      showSpinner(video);

      var options =
      {
        localVideo: video
      }

      webRtcPeer = kurentoUtils.WebRtcPeer.WebRtcPeerSendonly(options,
      function(error)
      {
        if(error) return onError(error)

        this.generateOffer(onOfferMaster)
      });

      webRtcPeer.on('icecandidate', function(candidate) {
        rpcBuilder.encode('candidate', [candidate])
      });
    }
  })
  btnViewer.addEventListener('click', function()
  {
    if (!webRtcPeer) {
      showSpinner(video);

      var options =
      {
        remoteVideo: video
      }

      webRtcPeer = kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options,
      function(error)
      {
        if(error) return onError(error)

        this.generateOffer(onOfferViewer)
      });

      webRtcPeer.on('icecandidate', function(candidate) {
        rpcBuilder.encode('candidate', [candidate])
      });
    }
  })
  btnTerminate.addEventListener('click', function()
  {
    rpcBuilder.encode('stop');

    dispose();
  })


  function onOfferMaster(error, offerSdp) {
    if(error) return onError(error)

    rpcBuilder.encode('master', [offerSdp], response);
  }

  function onOfferViewer(error, offerSdp) {
    if(error) return onError(error)

    rpcBuilder.encode('viewer', [offerSdp], response);
  }


  function dispose() {
    if (webRtcPeer) {
      webRtcPeer.dispose();
      webRtcPeer = null;
    }

    hideSpinner(video);
  }

  function onRequest(request) {
    switch(request.method)
    {
      case 'candidate':
        webRtcPeer.addIceCandidate(request.params[0])
      break;

      case 'stopCommunication':
        dispose();
      break;

      default:
        console.error('Unrecognized message', request);
    }
  }

  function response(error, sdpAnswer)
  {
    if(error)
    {
      onError(error);

      return dispose();
    }

    webRtcPeer.processAnswer(sdpAnswer);
  }
}


function onError(error) {
  if(error) console.error(error);
}

function showSpinner() {
  for (var i = 0; i < arguments.length; i++) {
    arguments[i].poster = './img/transparent-1px.png';
    arguments[i].style.background = 'center transparent url("./img/spinner.gif") no-repeat';
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
