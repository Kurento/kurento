function processOffer(peerConnection, offer, onsuccess, onerror)
{
  offer = new RTCSessionDescription({sdp: offer, type: 'offer'});

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
    onerror);
  },
  onerror);
};


window.addEventListener('load', function()
{
  KwsMedia('ws://localhost:8001', function(kwsMedia)
  {
    // Create pipeline
    kwsMedia.createMediaPipeline(function(error, pipeline)
    {
      if(error) return console.error(error);

      // Create pipeline media elements (endpoints & filters)
      var type = ['PlayerEndPoint', 'JackVaderFilter', 'WebRtcEndPoint'];
      var params = [{uri: "http://localhost:8000/video.avi"}, null, null];

      pipeline.createMediaElement(type, params,
      function(error, mediaElements)
      {
        if(error) return console.error(error);

        var playerEndPoint  = mediaElements[0];
        var jackVaderFilter = mediaElements[1];
        var webRtcEndPoint  = mediaElements[2];

        // Connect media element between them
        pipeline.connect(playerEndPoint, jackVaderFilter, webRtcEndPoint);

        // Subscribe to PlayerEndPoint EOS event
        playerEndPoint.on('EOS', function()
        {
          console.log("EOS");
        });

        // Create a PeerConnection client in the browser
        var peerConnection = new RTCPeerConnection
        (
          {iceServers: [{url: 'stun:stun.l.google.com:19302'}]},
          {optional:   [{DtlsSrtpKeyAgreement: true}]}
        );

        // Connect the pipeline to the PeerConnection client
        webRtcEndPoint.invoke("generateOffer", function(error, offer)
        {
          if(error) return console.error(error);

          processOffer(peerConnection, offer, function()
          {
            webRtcEndPoint.invoke("processAnswer", {answer: answer},
            function(error)
            {
              if(error) return console.error(error);

              var stream = peerConnection.getLocalStreams()[0];

              // Set the stream on the video tag
              var videoOutput = document.getElementById("videoOutput");
                  videoOutput.src = URL.createObjectURL(stream);

              // Start player
              playerEndPoint.start();
            });
          });

        });
      });
    });
  });
});
