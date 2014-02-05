var WebRtcEndpoint = KwsMedia.endpoints.WebRtcEndpoint;


getUserMedia({'audio': true, 'video': true}, function(stream)
{
  var videoInput  = document.getElementById("videoInput");
  var videoOutput = document.getElementById("videoOutput");

  videoInput.src = URL.createObjectURL(stream);

  KwsMedia('ws://192.168.0.110:7788/thrift/ws/websocket',
  function(kwsMedia)
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

        var constraints =
        {
          'mandatory':
          {
            'OfferToReceiveAudio':true,
            'OfferToReceiveVideo':true
          }
        };

        peerConnection.createOffer(function(offer)
        {
          console.log('offer', offer.sdp);

          peerConnection.setLocalDescription(offer, function()
          {
            console.log('setLocalDescription');
          },
          console.error);
        },
        console.error,
        constraints);

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
            });
          });
        });
      });
    });
  },
  function(error)
  {
    console.error('An error ocurred:',error);
  });
},
function(error)
{
  console.error('An error ocurred:',error);
});