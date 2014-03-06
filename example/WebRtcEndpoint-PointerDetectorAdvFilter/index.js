var WebRtcEndpoint         = KwsMedia.endpoints.WebRtcEndpoint;
var PointerDetector2Filter = KwsMedia.filters.PointerDetector2Filter;


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


getUserMedia({'audio': true, 'video': true}, function(stream)
{
  var videoInput  = document.getElementById("videoInput");
  var videoOutput = document.getElementById("videoOutput");

  videoInput.src = URL.createObjectURL(stream);

  KwsMedia('ws://192.168.0.106:7788/thrift/ws/websocket',
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

        PointerDetector2Filter.create(pipeline,
        function(error, pointerDetector2)
        {
          if(error) return console.error(error);

          pipeline.connect(webRtc, pointerDetector2);
          pipeline.connect(pointerDetector2, webRtc);  // loopback

          // Create a PeerConnection client in the browser
          var peerConnection = new RTCPeerConnection
          (
            {iceServers: [{url: 'stun:stun.l.google.com:19302'}]},
            {optional:   [{DtlsSrtpKeyAgreement: true}]}
          );

          peerConnection.addStream(stream);

          // Connect the pipeline to the PeerConnection client
          webRtc.generateOffer(function(error, offer)
          {
            if(error) return console.error(error);

            processOffer(peerConnection, offer, function(answer)
            {
              webRtc.processAnswer(answer, function(error)
              {
                if(error) return console.error(error);

                var stream = peerConnection.getRemoteStreams()[0];

                // Set the stream on the video tag
                videoOutput.src = URL.createObjectURL(stream);
              });
            },
            console.error);
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