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

const ws_uri = 'ws://130.206.81.87/thrift/ws/websocket';


function processOffer(peerConnection, offer, onsuccess, onerror)
{
  offer = new RTCSessionDescription({sdp: offer, type: 'offer'});

  var constraints =
  {
    'mandatory':
    {
      'OfferToReceiveAudio':true,
      'OfferToReceiveVideo':true
    }
  };

  peerConnection.setRemoteDescription(offer, function()
  {
    console.log(offer.sdp);

    peerConnection.createAnswer(function(answer)
    {
      console.log(answer.sdp);

      peerConnection.setLocalDescription(answer, function()
      {
        onsuccess(answer.sdp);
      },
      onerror);
    },
    onerror,
    constraints);
  },
  onerror);
};

function onerror(error)
{
  console.error(error);
};


getUserMedia({'audio': true, 'video': true}, function(stream)
{
  var videoInput  = document.getElementById("videoInput");
  var videoOutput = document.getElementById("videoOutput");

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

        // Connect the pipeline to the PeerConnection client
        webRtc.generateOffer(function(error, offer)
        {
          if(error) return onerror(error);

          // Create a PeerConnection client in the browser
          var peerConnection = new RTCPeerConnection
          (
            {iceServers: [{url: 'stun:stun.l.google.com:19302'}]},
            {optional:   [{DtlsSrtpKeyAgreement: true}]}
          );

          peerConnection.addStream(stream);

          processOffer(peerConnection, offer, function(answer)
          {
            webRtc.processAnswer(answer, function(error)
            {
              if(error) return onerror(error);

              var stream = peerConnection.getRemoteStreams()[0];

              // Set the stream on the video tag
              videoOutput.src = URL.createObjectURL(stream);

             // loopback
              webRtc.connect(webRtc, function(error)
              {
                if(error) return onerror(error);

                console.log('loopback established');
              });
            });
          },
          onerror);
        });
      });
    });
  },
  onerror);
},
onerror);
