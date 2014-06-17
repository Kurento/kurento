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

const overlayImage = 'http://files.kurento.org/imgs/mario-wings.png';


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

  KwsMedia(ws_uri)
  .then(function(kwsMedia)
  {
    // Create pipeline
    return kwsMedia.create('MediaPipeline');
  })
  .then(function(pipeline)
  {
    // Create pipeline media elements (endpoints & filters)
    return Promise.all(
    [
      pipeline.create('WebRtcEndpoint')
    ,
      pipeline.create('FaceOverlayFilter')
      .then(function(faceOverlay)
      {
        var uri = overlayImage;
        var offsetXPercent = -0.4;
        var offsetYPercent = -1.2;
        var widthPercent   =  1.8;
        var heightPercent  =  1.8;

        return faceOverlay.setOverlayedImage(uri, offsetXPercent, offsetYPercent,
                                             widthPercent, heightPercent)
      })
    ]);
  })
  .then(function(values)
  {
    var webRtc      = values[0];
    var faceOverlay = values[1];

    var peerConnection;

    return Promise.all(
    [
      webRtc.connect(faceOverlay)
    ,
      new Promise(function(resolve, reject)
      {
        // Create a PeerConnection client in the browser
        peerConnection = new RTCPeerConnection
        (
          {iceServers: [{url: 'stun:stun.l.google.com:19302'}]},
          {optional:   [{DtlsSrtpKeyAgreement: true}]}
        );

        peerConnection.addStream(stream);

        createOffer(peerConnection, reject);

        peerConnection.addEventListener('icecandidate', function(event)
        {
          if(event.candidate) return;

          var offer = peerConnection.localDescription.sdp;

          console.log('offer+candidates', offer);

          // Connect the pipeline to the PeerConnection client
          resolve(offer);
        });
      })
      .then(webRtc.processOffer.bind(webRtc))
      .then(function(answer)
      {
        console.log('answer', answer);

        answer = new RTCSessionDescription({sdp: answer, type: 'answer'});

        return new Promise(function(resolve, reject)
        {
          peerConnection.setRemoteDescription(answer, function()
          {
            var stream = peerConnection.getRemoteStreams()[0];

            resolve(stream);
          },
          reject);
        });
      })
    ,
      // loopback
      faceOverlay.connect(webRtc)
    ])
    .then(function(values)
    {
      return values[1];
    });
  })
  .then(function(stream)
  {
    // Set the stream on the video tag
    videoOutput.src = URL.createObjectURL(stream);
  })
  .then(function()
  {
    console.log('loopback established');
  })
  .catch(onerror);
},
onerror);
