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

const ws_uri = 'ws://kms01.kurento.org:8080/thrift/ws/websocket';


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


function onerror(error)
{
  console.error(error);
};


getUserMedia({audio: true, video: true}, function(stream)
{
  var videoInput  = document.getElementById('videoInput');
  var videoOutput = document.getElementById('videoOutput');

  videoInput.src = URL.createObjectURL(stream);

  KwsMedia(ws_uri, function(kwsMedia)
  {
    // Create pipeline
    kwsMedia.create('MediaPipeline', function(error, pipeline)
    {
      if(error) return onerror(error);

      // Create pipeline media elements (endpoints & filters)
      pipeline.create('WebRtcEndpoint', function(error, webRtc)
      {
        if(error) return onerror(error);

        var calibrationRegion =
        {
          topRightCornerX: 0,
          topRightCornerY: 0,
          width: 50,
          height: 50
        };

        pipeline.create('FaceOverlayFilter', function(error, faceOverlay)
        {
          if(error) return onerror(error);

          var uri = 'http://www.clipartbest.com/cliparts/di6/eeL/di6eeLgkT.svg';
          var offsetXPercent = 0.0;
          var offsetYPercent = 0.0;
          var widthPercent   = 1.0;
          var heightPercent  = 1.0;

          faceOverlay.setOverlayedImage(uri, offsetXPercent, offsetYPercent,
                                        widthPercent, heightPercent,
                                        function(error)
          {
            if(error) return onerror(error);

            webRtc.connect(faceOverlay, function(error)
            {
              if(error) return onerror(error);

              // Create a PeerConnection client in the browser
              var peerConnection = new RTCPeerConnection
              (
                {iceServers: [{url: 'stun:stun.l.google.com:19302'}]},
                {optional:   [{DtlsSrtpKeyAgreement: true}]}
              );

              peerConnection.addStream(stream);

              createOffer(peerConnection, onerror);

              peerConnection.addEventListener('icecandidate', function(event)
              {
                if(event.candidate) return;

                var offer = peerConnection.localDescription;

                console.log('offer+candidates', offer.sdp);

                // Connect the pipeline to the PeerConnection client
                webRtc.processOffer(offer.sdp, function(error, answer)
                {
                  if(error) return onerror(error);

                  var descriptionInitDict = {sdp: answer, type: 'answer'};

                  answer = new RTCSessionDescription(descriptionInitDict);

                  console.log('answer', answer.sdp);

                  peerConnection.setRemoteDescription(answer, function()
                  {
                    var stream = peerConnection.getRemoteStreams()[0];

                    // Set the stream on the video tag
                    videoOutput.src = URL.createObjectURL(stream);

                    // loopback
                    faceOverlay.connect(webRtc, function(error)
                    {
                      if(error) return onerror(error);

                      console.log('loopback established');
                    });
                  },
                  onerror);
                });
              });
            });
          });
        });
      });
    });
  },
  onerror);
},
onerror);
