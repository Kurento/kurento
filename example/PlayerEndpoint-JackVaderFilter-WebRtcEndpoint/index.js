var PlayerEndpoint  = KwsMedia.endpoints.PlayerEndpoint;
var JackVaderFilter = KwsMedia.filters.JackVaderFilter;
var WebRtcEndpoint  = KwsMedia.endpoints.WebRtcEndpoint;


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
  var videoOutput = document.getElementById("videoOutput");

  KwsMedia('ws://192.168.0.110:7788/thrift/ws/websocket',
  function(kwsMedia)
  {
    // Create pipeline
    kwsMedia.createMediaPipeline(function(error, pipeline)
    {
      if(error) return console.error(error);

      // Create pipeline media elements (endpoints & filters)
      PlayerEndpoint.create(pipeline,
      {uri: "https://ci.kurento.com/video/fiwarecut.webm"},
      function(error, player)
      {
        if(error) return console.error(error);

        // Subscribe to PlayerEndpoint EOS event
        player.on('EndOfStream', function(event)
        {
          console.log("EndOfStream event:", event);
        });

        JackVaderFilter.create(pipeline, function(error, jackVader)
        {
          if(error) return console.error(error);

          // Connect media element between them
          pipeline.connect(player, jackVader, function(error, pipeline)
          {
            if(error) return console.error(error);

            WebRtcEndpoint.create(pipeline, function(error, webRtc)
            {
              if(error) return console.error(error);

              // Connect media element between them
              pipeline.connect(jackVader, webRtc, function(error, pipeline)
              {
                // Connect the pipeline to the PeerConnection client
                webRtc.generateOffer(function(error, offer)
                {
                  if(error) return console.error(error);

                  // Create a PeerConnection client in the browser
                  var peerConnection = new RTCPeerConnection
                  (
                    {iceServers: [{url: 'stun:stun.l.google.com:19302'}]},
                    {optional:   [{DtlsSrtpKeyAgreement: true}]}
                  );

                  processOffer(peerConnection, offer, function(answer)
                  {
                    webRtc.processAnswer(answer, function(error)
                    {
                      if(error) return console.error(error);

                      var stream = peerConnection.getRemoteStreams()[0];

                      // Set the stream on the video tag
                      videoOutput.src = URL.createObjectURL(stream);

                      // Start player
                      player.play(function(error, result)
                      {
                        if(error) return console.error(error);

                        console.log(result);
                      });
                    });
                  },
                  console.error);
                });
              });
            });
          });
        });
      });
    });
  },
  console.error);
});