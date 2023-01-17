/*
 * Copyright 2018 Kurento (https://www.kurento.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

const ws = new WebSocket('wss://' + location.host + '/rtpreceiver');

let videoRtp;
let webRtcPeer;

// UI
let uiState = null;
const UI_IDLE = 0;
const UI_STARTING = 1;
const UI_STARTED = 2;

window.onload = function()
{
  console = new Console();
  console.log("Page loaded");
  videoRtp = document.getElementById('videoRtp');
  uiSetState(UI_IDLE);
}

window.onbeforeunload = function()
{
  ws.close();
}

function explainUserMediaError(err)
{
  const n = err.name;
  if (n === 'NotFoundError' || n === 'DevicesNotFoundError') {
    return "Missing webcam for required tracks";
  }
  else if (n === 'NotReadableError' || n === 'TrackStartError') {
    return "Webcam is already in use";
  }
  else if (n === 'OverconstrainedError' || n === 'ConstraintNotSatisfiedError') {
    return "Webcam doesn't provide required tracks";
  }
  else if (n === 'NotAllowedError' || n === 'PermissionDeniedError') {
    return "Webcam permission has been denied by the user";
  }
  else if (n === 'TypeError') {
    return "No media tracks have been requested";
  }
  else {
    return "Unknown error";
  }
}

function sendMessage(message)
{
  const jsonMessage = JSON.stringify(message);
  console.log("[sendMessage] message: " + jsonMessage);
  ws.send(jsonMessage);
}



/* ============================= */
/* ==== WebSocket signaling ==== */
/* ============================= */

ws.onmessage = function(message)
{
  const jsonMessage = JSON.parse(message.data);
  console.log("[onmessage] Received message: " + message.data);

  switch (jsonMessage.id) {
    case 'PROCESS_SDP_ANSWER':
      handleProcessSdpAnswer(jsonMessage);
      break;
    case 'ADD_ICE_CANDIDATE':
      handleAddIceCandidate(jsonMessage);
      break;
    case 'SHOW_CONN_INFO':
      handleShowConnInfo(jsonMessage);
      break;
    case 'SHOW_SDP_ANSWER':
      handleShowSdpAnswer(jsonMessage);
      break;
    case 'END_PLAYBACK':
      handleEndPlayback(jsonMessage);
      break;
    case 'ERROR':
      handleError(jsonMessage);
      break;
    default:
    error("[onmessage] Invalid message, id: " + jsonMessage.id);
      break;
  }
}

// PROCESS_SDP_ANSWER ----------------------------------------------------------

function handleProcessSdpAnswer(jsonMessage)
{
  console.log("[handleProcessSdpAnswer] SDP Answer received from Kurento Client; process in Kurento Peer");

  webRtcPeer.processAnswer(jsonMessage.sdpAnswer, (err) => {
    if (err) {
      console.error("[handleProcessSdpAnswer] " + err);
      return;
    }

    console.log("[handleProcessSdpAnswer] SDP Answer ready; start remote video");
    startVideo(videoRtp);

    uiSetState(UI_STARTED);
  });
}

// ADD_ICE_CANDIDATE -----------------------------------------------------------

function handleAddIceCandidate(jsonMessage)
{
  webRtcPeer.addIceCandidate(jsonMessage.candidate, (err) => {
    if (err) {
      console.error("[handleAddIceCandidate] " + err);
      return;
    }
  });
}

// SHOW_CONN_INFO --------------------------------------------------------------

function handleShowConnInfo(jsonMessage)
{
  document.getElementById("msgConnInfo").value = jsonMessage.text;
}

// SHOW_SDP_ANSWER -------------------------------------------------------------

function handleShowSdpAnswer(jsonMessage)
{
  document.getElementById("msgSdpText").value = jsonMessage.text;
}

// END_PLAYBACK ----------------------------------------------------------------

function handleEndPlayback(jsonMessage)
{
  uiSetState(UI_IDLE);
  hideSpinner(videoRtp);
}

// ERROR -----------------------------------------------------------------------

function error(errMessage)
{
  console.error("[error] " + errMessage);
  if (uiState == UI_STARTING) {
    uiSetState(UI_IDLE);
  }
}

function handleError(jsonMessage)
{
  const errMessage = jsonMessage.message;
  error(errMessage);
}



/* ==================== */
/* ==== UI actions ==== */
/* ==================== */

// start -----------------------------------------------------------------------

function start()
{
  console.log("[start] Create WebRtcPeerRecvonly");
  uiSetState(UI_STARTING);
  showSpinner(videoRtp);

  const options = {
    localVideo: null,
    remoteVideo: videoRtp,
    mediaConstraints: { audio: true, video: true },
    onicecandidate: (candidate) => sendMessage({
      id: 'ADD_ICE_CANDIDATE',
      candidate: candidate,
    }),
  };

  webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options,
      function(err)
  {
    if (err) {
      console.error("[start/WebRtcPeerRecvonly] Error in constructor: "
          + explainUserMediaError(err));
      return;
    }

    console.log("[start/WebRtcPeerRecvonly] Created; generate SDP Offer");
    webRtcPeer.generateOffer((err, sdpOffer) => {
      if (err) {
        console.error("[start/WebRtcPeerRecvonly/generateOffer] " + err);
        return;
      }

      const useComedia = document.getElementById('useComedia').checked;
      const useSrtp = document.getElementById('useSrtp').checked;

      console.log("[start/WebRtcPeerRecvonly/generateOffer] Use COMEDIA: "
          + useComedia);
      console.log("[start/WebRtcPeerRecvonly/generateOffer] Use SRTP: "
          + useSrtp);

      sendMessage({
        id: 'PROCESS_SDP_OFFER',
        sdpOffer: sdpOffer,
        useComedia: useComedia,
        useSrtp: useSrtp,
      });

      console.log("[start/WebRtcPeerRecvonly/generateOffer] Done!");
      uiSetState(UI_STARTED);
    });
  });
}

// stop ------------------------------------------------------------------------

function stop()
{
  console.log("[stop]");

  sendMessage({
    id: 'STOP',
  });

  if (webRtcPeer) {
    webRtcPeer.dispose();
    webRtcPeer = null;
  }

  uiSetState(UI_IDLE);
  hideSpinner(videoRtp);
}



/* ================== */
/* ==== UI state ==== */
/* ================== */

function uiSetState(nextState)
{
  switch (nextState) {
    case UI_IDLE:
      uiEnableElement('#start', 'start()');
      uiDisableElement('#stop');
      break;
    case UI_STARTING:
      uiDisableElement('#start');
      uiDisableElement('#stop');
      break;
    case UI_STARTED:
      uiDisableElement('#start');
      uiEnableElement('#stop', 'stop()');
      break;
    default:
      console.error("[setState] Unknown state: " + nextState);
      return;
  }
  uiState = nextState;
}

function uiEnableElement(id, onclickHandler)
{
  $(id).attr('disabled', false);
  if (onclickHandler) {
    $(id).attr('onclick', onclickHandler);
  }
}

function uiDisableElement(id)
{
  $(id).attr('disabled', true);
  $(id).removeAttr('onclick');
}

function showSpinner()
{
  for (let i = 0; i < arguments.length; i++) {
    arguments[i].poster = './img/transparent-1px.png';
    arguments[i].style.background = "center transparent url('./img/spinner.gif') no-repeat";
  }
}

function hideSpinner()
{
  for (let i = 0; i < arguments.length; i++) {
    arguments[i].src = '';
    arguments[i].poster = './img/webrtc.png';
    arguments[i].style.background = '';
  }
}

function startVideo(video)
{
  // Manually start the <video> HTML element
  // This is used instead of the 'autoplay' attribute, because iOS Safari
  //  requires a direct user interaction in order to play a video with audio.
  // Ref: https://developer.mozilla.org/en-US/docs/Web/HTML/Element/video
  video.play().catch((err) => {
    if (err.name === 'NotAllowedError') {
      console.error("[start] Browser doesn't allow playing video: " + err);
    }
    else {
      console.error("[start] Error in video.play(): " + err);
    }
  });
}

/**
 * Lightbox utility (to display media pipeline image in a modal dialog)
 */
$(document).delegate('*[data-toggle="lightbox"]', 'click', function(event) {
  event.preventDefault();
  $(this).ekkoLightbox();
});
