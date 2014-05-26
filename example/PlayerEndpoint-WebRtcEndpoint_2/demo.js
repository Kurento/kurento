const ws_uri = 'ws://192.168.0.105:7788/thrift/ws/websocket';

const URL_FIWARECUT = "https://ci.kurento.com/video/fiwarecut.webm";


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
      PlayerEndpoint.create(pipeline, {uri: URL_FIWARECUT},
      function(error, player)
      {
        if(error) return onerror(error);

        // Subscribe to PlayerEndpoint EOS event
        player.on('EndOfStream', function(event)
        {
          console.log("EndOfStream event:", event);
        });

        pipeline.create('WebRtcEndpoint', function(error, webRtc)
        {
          if(error) return onerror(error);

          // Connect media element between them
          player.connect(webRtc, function(error)
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
                },
                onerror);
              });
            });
          });
        });
      });
    });
  },
  onerror);
});
