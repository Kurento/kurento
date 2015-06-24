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

var videoInput;
var videoOutput;
var webRtcPeer;


const NOT_REGISTERED = 0;
const REGISTERING = 1;
const REGISTERED = 2;

const NO_CALL = 0;
const PROCESSING_CALL = 1;
const IN_CALL =2;

var registerName = null;
var registerState = null
var callState = null


function setRegisterState(nextState){
  switch (nextState) {
	  case NOT_REGISTERED:
	    $('#register').attr('disabled', false);

	    $('#call').attr('disabled', true);
	    $('#terminate').attr('disabled', true);
	  break;

	  case REGISTERING:
	    $('#register').attr('disabled', true);
	  break;

	  case REGISTERED:
	    $('#register').attr('disabled', true);
	    setCallState(NO_CALL);
	  break;

	  default: return;
  }

  registerState = nextState;
}

function setCallState(nextState){
  switch (nextState) {
	  case NO_CALL:
	    $('#call').attr('disabled', false);
	    $('#terminate').attr('disabled', true);
    break;

	  case PROCESSING_CALL:
	    $('#call').attr('disabled', true);
	    $('#terminate').attr('disabled', true);
    break;

	  case IN_CALL:
	    $('#call').attr('disabled', true);
	    $('#terminate').attr('disabled', false);
    break;

	  default: return;
  }

  callState = nextState;
}


window.onload = function() {
  console = new Console();

  setRegisterState(NOT_REGISTERED);

  const packer = RpcBuilder.packers.JsonRPC;

  var options = {request_timeout: 60*1000}
  var ws = new WebSocket('ws:'+location.host)
  var rpcBuilder = new RpcBuilder(packer, options, ws, onRequest);

  window.onbeforeunload = rpcBuilder.close.bind(rpcBuilder);

  videoInput = document.getElementById('videoInput');
  videoOutput = document.getElementById('videoOutput');

  var txtName = document.getElementById('name')
  var txtPeer = document.getElementById('peer')

  var btnRegister = document.getElementById('register')
  var btnCall = document.getElementById('call')
  var btnTerminate = document.getElementById('terminate')

  txtName.addEventListener('keydown', function(event)
  {
    if(event.keyCode == 13) register()
  })
  btnRegister.addEventListener('click', register)

  txtPeer.addEventListener('keydown', function(event)
  {
    if(event.keyCode == 13) call()
  })
  btnCall.addEventListener('click', call)
  btnTerminate.addEventListener('click', function(event)
  {
    rpcBuilder.encode('stop', registerName)
    stop()
  })


  function register()
  {
    var name = txtName.value;
    if(!name) return window.alert("You must insert your user name");

    rpcBuilder.encode('register', name, registerResponse);

    registerName = name;
    setRegisterState(REGISTERING);
    txtPeer.focus();
  }

  function call()
  {
    if(!txtPeer.value) return window.alert("You must specify the peer name");

    var options =
    {
      localVideo: videoInput,
      remoteVideo: videoOutput
    }

    webRtcPeer = kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options,
      function(error)
    {
      if(error)
      {
        setCallState(NO_CALL);
        return onError(error)
      }

      this.generateOffer(onOfferCall)
    });

    webRtcPeer.on('icecandidate', function(candidate) {
      rpcBuilder.encode('candidate', [candidate])
    });

    setCallState(PROCESSING_CALL);
    showSpinner(videoInput, videoOutput);
  }

  function onOfferCall(error, sdpOffer)
  {
    if(error) return onError(error)

    console.log('Invoking SDP offer callback function');

    rpcBuilder.encode('call', [txtPeer.value, sdpOffer], callResponse);
  }


  function onRequest(request)
  {
    console.info('Received message',request);

    switch(request.method)
    {
      case 'candidate':
        webRtcPeer.addIceCandidate(request.params[0])
      break;

      case 'call':
        incomingCall(request);
        break;

      case 'stop':
        console.info("Communication ended by remote peer");
        stop();
        break;

      default:
        console.error('Unrecognized message', request);
    }
  }

  function incomingCall(request)
  {
    //If bussy just reject without disturbing user
    if(callState != NO_CALL) return request.reply('bussy');

    setCallState(PROCESSING_CALL);

    var from = request.params[0]

    if(confirm('User '+from+' is calling you. Do you accept the call?'))
    {
      showSpinner(videoInput, videoOutput);

      function onError(error) {
        if(error)
        {
          console.error(error);
          request.reply(error);
        }
      }

      var options =
      {
        localVideo: videoInput,
        remoteVideo: videoOutput
      }

      webRtcPeer = kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options,
      function(error)
      {
        if(error) return onError(error)

        this.generateOffer(onOfferIncoming)
      })

      webRtcPeer.on('icecandidate', function(candidate) {
        rpcBuilder.encode('candidate', [candidate])
      });
    }
    else
    {
      request.reply('user declined');
      stop();
    }

    function onOfferIncoming(error, sdpOffer)
    {
      if(error) return onError(error)

      request.reply();
      rpcBuilder.encode('callResponse', [from, sdpOffer], callResponse);
    };
  }


  new Draggabilly(document.getElementById('videoSmall'));
  txtName.focus();
}


function registerResponse(error)
{
  if(error)
  {
    setRegisterState(NOT_REGISTERED);
    console.error(error);
    return alert('Error registering user. See console for further information.');
  }

  setRegisterState(REGISTERED);
}

function callResponse(error, sdpAnswer)
{
  if(error)
  {
    console.error(error);
    return stop();
  }

  setCallState(IN_CALL);
  webRtcPeer.processAnswer(sdpAnswer);
}


function stop(message)
{
  setCallState(NO_CALL);

  if(webRtcPeer)
  {
    webRtcPeer.dispose();
    webRtcPeer = null;
  }

  hideSpinner(videoInput, videoOutput);
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
