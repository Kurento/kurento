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

const ws_uri = 'ws://demo01.kurento.org:8080/thrift/ws/websocket';

const URL_FIWARECUT = "http://files.kurento.org/video/fiwarecut.webm";


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


window.addEventListener('load', function()
{
  var videoOutput = document.getElementById("videoOutput");

  KwsMedia(ws_uri, function(kwsMedia)
  {
    // Create pipeline
    kwsMedia.create('MediaPipeline', function(error, pipeline)
    {
      if(error) return onerror(error);

      // Create pipeline media elements (endpoints & filters)
      pipeline.create('PlayerEndpoint', {uri: URL_FIWARECUT},
      function(error, player)
      {
        if(error) return onerror(error);

        // Subscribe to PlayerEndpoint EOS event
        player.on('EndOfStream', function(event)
        {
          console.log("EndOfStream event:", event);
        });

        pipeline.create('JackVaderFilter', function(error, jackVader)
        {
          if(error) return onerror(error);

          // Connect media element between them
          player.connect(jackVader, function(error)
          {
            if(error) return onerror(error);

            pipeline.create('WebRtcEndpoint', function(error, webRtc)
            {
              if(error) return onerror(error);

              // Connect media element between them
              jackVader.connect(webRtc, function(error, pipeline)
              {
                // Create a PeerConnection client in the browser
                var peerConnection = new RTCPeerConnection
                (
                  {iceServers: [{url: 'stun:stun.l.google.com:19302'}]},
                  {optional:   [{DtlsSrtpKeyAgreement: true}]}
                );

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

                    answer = new RTCSessionDescription({sdp: answer, type: 'answer'});

                    console.log('answer', answer.sdp);

                    peerConnection.setRemoteDescription(answer, function()
                    {
                      var stream = peerConnection.getRemoteStreams()[0];

                      // Set the stream on the video tag
                      videoOutput.src = URL.createObjectURL(stream);

                      // Start player
                      player.play(function(error)
                      {
                        if(error) return onerror(error);

                        console.log('Play');
                      });
                    });
                  });
                });
              });
            });
          });
        });
      });
    });
  },
  onerror);
});
