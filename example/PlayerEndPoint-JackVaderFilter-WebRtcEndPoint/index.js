var RTCPeerConnection = mozRTCPeerConnection || RTCPeerConnection || webkitRTCPeerConnection;


var PlayerEndPoint    = KwsMedia.endpoints.PlayerEndPoint;
var JackVaderFilter = KwsMedia.filters.JackVaderFilter;
var WebRtcEndPoint    = KwsMedia.endpoints.WebRtcEndPoint;


window.addEventListener('load', function()
{
  KwsMedia('ws://192.168.0.110:7788/thrift/ws/websocket',
  function(kwsMedia)
  {
    // Create pipeline
    kwsMedia.createMediaPipeline(function(error, pipeline)
    {
      if(error) return console.error(error);

      // Create pipeline media elements (endpoints & filters)
      Player.createMediaElement(pipeline, {uri: "http://localhost:8000/video.avi"},
      function(error, player)
      {
        if(error) return console.error(error);

        JackVader.createMediaElement(pipeline, function(error, jackVader)
        {
          if(error) return console.error(error);

          WebRtc.createMediaElement(pipeline, function(error, webRtc)
          {
            if(error) return console.error(error);

            // Connect media element between them
            pipeline.connect(player, jackVader, webRtc);

            // Subscribe to PlayerEndPoint EOS event
            player.on('EOS', function()
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
            webRtc.generateSdpOffer(function(error, offer)
            {
              if(error) return console.error(error);

              peerConnection.setRemoteDescription(
              new RTCSessionDescription({sdp: offer, type: 'offer'}),
              function()
              {
                peerConnection.createAnswer(function(answer)
                {
                  peerConnection.setLocalDescription(answer, function()
                  {

                    webRtc.processSdpAnswer(answer, function(error)
                    {
                      if(error) return console.error(error);

                      var stream = peerConnection.getRemoteStreams()[0];

                      // Set the stream on the video tag
                      var videoOutput = document.getElementById("videoOutput");
                          videoOutput.src = URL.createObjectURL(stream);

                      // Start player
                      player.start();
                    });

                  },
                  console.error);
                },
                console.error);
              },
              console.error);

            });
          });
        });
      });

    });
  });
});