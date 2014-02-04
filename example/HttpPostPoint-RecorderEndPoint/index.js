var RTCPeerConnection = RTCPeerConnection || webkitRTCPeerConnection;


getUserMedia({'audio': true, 'video': true}, function(stream)
{
  var videoInput = document.getElementById("videoInput");
      videoInput.src = URL.createObjectURL(stream);


  KwsMedia('ws://192.168.0.110:7788/thrift/ws/websocket',
//  KwsMedia('ws://179.111.137.126:7788/thrift/ws/websocket',
  function(kwsMedia)
  {
    // Create pipeline
    kwsMedia.createMediaPipeline(function(error, pipeline)
    {
      if(error) return console.error(error);

      // Create pipeline media elements (endpoints & filters)
      pipeline.createMediaElement(['HttpPostEndPoint', 'RecorderEndPoint'],
      function(error, mediaElements)
      {
        if(error) return console.error(error);

        var httpPostEndPoint = mediaElements[0];
        var recorderEndPoint = mediaElements[1];

        // Connect media element between them
        pipeline.connect(httpPostEndPoint, recorderEndPoint,
        function(error, pipeline)
        {
          if(error) return console.error(error);

          console.log(pipeline);
        });

        httpPostEndPoint.getUrl(function(error, url)
        {
          if(error) return console.error(error);

          // Set the video on the video tag
          var xhr = new XmlHttpRequest();
              xhr.open('post', url);
              xhr.send(stream);

          console.log(url);

          // Start recorder
          recorderEndPoint.record(function(error, result)
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