var RTCPeerConnection = RTCPeerConnection || webkitRTCPeerConnection;


window.addEventListener('load', function()
{
  KwsMedia('ws://192.168.0.110:7788/thrift/ws/websocket',
//  KwsMedia('ws://179.111.137.126:7788/thrift/ws/websocket',
  function(kwsMedia)
  {
    // Create pipeline
    kwsMedia.createMediaPipeline(function(error, pipeline)
    {
      if(error) return console.error(error);

      // Create pipeline media elements (endpoints & filters)
      var type   = ['PlayerEndPoint', 'HttpGetEndPoint'];
      var params = [{uri: "http://localhost:8000/video.avi"}, null];

      pipeline.createMediaElement(type, params,
      function(error, mediaElements)
      {
        if(error) return console.error(error);

        var playerEndPoint  = mediaElements[0];
        var httpGetEndPoint = mediaElements[1];

        // Connect media element between them
        pipeline.connect(playerEndPoint, httpGetEndPoint,
        function(error, pipeline)
        {
          if(error) return console.error(error);

          console.log(pipeline);
        });

        // Subscribe to PlayerEndPoint EOS event
        playerEndPoint.on('EOS', function()
        {
          console.log("EOS");
        });

        // Set the video on the video tag
        var videoOutput = document.getElementById("videoOutput");

        httpGetEndPoint.getUrl(function(error, url)
        {
          if(error) return console.error(error);

          videoOutput.src = url;
          console.log(url);

          // Start player
          playerEndPoint.start(function(error, result)
          {
            if(error) return console.error(error);

            console.log(result);
          });
        });

      });
    });
  },
  function(error)
  {
    console.error('An error ocurred:',error);
  });
});
