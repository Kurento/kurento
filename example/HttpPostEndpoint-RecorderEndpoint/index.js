var HttpPostEndpoint = KwsMedia.endpoints.HttpPostEndpoint;
var RecorderEndpoint = KwsMedia.endpoints.RecorderEndpoint;


getUserMedia({'audio': true, 'video': true}, function(stream)
{
  var videoInput = document.getElementById("videoInput");
      videoInput.src = URL.createObjectURL(stream);


  KwsMedia('ws://192.168.0.110:7788/thrift/ws/websocket',
  function(kwsMedia)
  {
    // Create pipeline
    kwsMedia.createMediaPipeline(function(error, pipeline)
    {
      if(error) return console.error(error);

      // Create pipeline media elements (endpoints & filters)
      HttpPostEndpoint.create(pipeline, function(httpPos)
      {
        if(error) return console.error(error);

        RecorderEndpoint.create(pipeline, function(error, recorder)
        {
          if(error) return console.error(error);

          // Connect media element between them
          pipeline.connect(httpPost, recorder,
          function(error, pipeline)
          {
            if(error) return console.error(error);

            console.log(pipeline);
          });

          httpPost.getUrl(function(error, url)
          {
            if(error) return console.error(error);

            // Set the video on the video tag
            var xhr = new XmlHttpRequest();
                xhr.open('post', url);
                xhr.send(stream);

            console.log(url);

            // Start recorder
            recorder.record(function(error, result)
            {
              if(error) return console.error(error);

              console.log(result);
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
});