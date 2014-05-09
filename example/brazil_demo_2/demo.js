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

const MAX_FRAMERATE = 1;


function createOffer(peerConnection, onerror)
{
  peerConnection.createOffer(function(offer)
  {
    peerConnection.setLocalDescription(offer, function()
    {
      console.log('offer', offer.sdp);
    },
    onerror);
  },
  onerror);
};


window.addEventListener('load', function()
{
  var buttonStart     = document.getElementById("start");
  var buttonTerminate = document.getElementById("terminate");
  var calibrate       = document.getElementById("calibrate");

  var remoteVideo = document.getElementById("remoteVideo");


  var cpbWebRtc = new CpbWebRtc();


  function terminate()
  {
    cpbWebRtc.terminate();

    remoteVideo.classList.remove("playing");
    remoteVideo.src = null;

    // Enable connect button
    buttonStart.disabled = false;
  };

  function onerror(error)
  {
    console.error(error);

    terminate();
  };


  buttonStart.addEventListener('click', function()
  {
    var constraints =
    {
      audio: true,
      video:
      {
        mandatory:
        {
          maxFrameRate: MAX_FRAMERATE
        }
      }
    };

    getUserMedia(constraints, function(stream)
    {
      var videoInput  = document.getElementById("videoInput");
      videoInput.src = URL.createObjectURL(stream);

      // Disable start button
      buttonStart.disabled = true;

      // Show spinners
      remoteVideo.classList.add("playing");

      var peerConnection = new RTCPeerConnection(
      {
        iceServers: [{url: 'stun:stun.l.google.com:19302'}]
      });

      peerConnection.addStream(stream);

      createOffer(peerConnection, onerror);

      peerConnection.addEventListener('icecandidate', function(event)
      {
        var candidate = event.candidate;

        // We are still generating the candidates, don't send the SDP yet.
        if(candidate) return console.debug(candidate);

        var offer = this.localDescription;

        console.debug('offer+candidates', offer.sdp);

        cpbWebRtc.start(offer, function(error, answer)
        {
          if(error) return onerror(error);

          answer = new RTCSessionDescription({type: 'answer', 'sdp': answer});

          console.debug('answer', answer.sdp);

          peerConnection.setRemoteDescription(answer, function()
          {
//            var stream = peerConnection.getRemoteStreams()[0];
//
//            // Set the stream on the video tag
//            remoteVideo.src = URL.createObjectURL(stream);
//
//            // loopback
//            pipeline.connect(pointerDetectorAdv, webRtc, function(error)
//            {
//              if(error) return console.error(error);
//
//              console.log('loopback established');
//            });
          });
        });
      });

      peerConnection.onsignalingstatechange = function(event)
      {
        if(this.signalingState == "stable")
        {
          var stream = this.getRemoteStreams()[0];

          if(stream)
          {
            remoteVideo.src = URL.createObjectURL(stream);

            buttonTerminate.disabled = false;

            console.log('* Creation completed *');
          }
          else
            console.error("No remote streams are available");
        };
      };
    },
    onerror);
  });

  buttonTerminate.addEventListener('click', function()
  {
    // Disable terminate button
    buttonTerminate.disabled = true;

    // Terminate the connection
    terminate();
  });

  calibrate.addEventListener('click', function()
  {
    cpbWebRtc.calibrate();
  });
});
