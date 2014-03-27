var WebRtcEndpoint = kwsMediaApi.endpoints.WebRtcEndpoint;


const ws_uri = 'ws://130.206.81.87/thrift/ws/websocket';


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


getUserMedia({'audio': true, 'video': true}, function(stream)
{
  var videoInput  = document.getElementById("videoInput");
  var videoOutput = document.getElementById("videoOutput");

  videoInput.src = URL.createObjectURL(stream);

  kwsMediaApi.KwsMedia(ws_uri, function(kwsMedia)
  {
    // Create pipeline
    kwsMedia.createMediaPipeline(function(error, pipeline)
    {
      if(error) return console.error(error);

      // Create pipeline media elements (endpoints & filters)
      WebRtcEndpoint.create(pipeline, function(error, webRtc)
      {
        if(error) return console.error(error);

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
            if(error) return console.error(error);

            answer = new RTCSessionDescription({sdp: answer, type: 'answer'});

            console.log('answer', answer.sdp);

            peerConnection.setRemoteDescription(answer, function()
            {
              var stream = peerConnection.getRemoteStreams()[0];

              // Set the stream on the video tag
              videoOutput.src = URL.createObjectURL(stream);

              // loopback
              pipeline.connect(webRtc, webRtc, function(error)
              {
                if(error) return console.error(error);

                console.log('loopback established');
              });
            },
            onerror);
          });
        });
      });
    });
  },
  onerror);
},
onerror);
