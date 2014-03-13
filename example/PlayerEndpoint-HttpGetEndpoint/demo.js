var PlayerEndpoint  = KwsMedia.endpoints.PlayerEndpoint;
var HttpGetEndpoint = KwsMedia.endpoints.HttpGetEndpoint;


window.addEventListener('load', function()
{
  var videoOutput = document.getElementById("videoOutput");

  KwsMedia('ws://130.206.81.87/thrift/ws/websocket',
  function(kwsMedia)
  {
    // Create pipeline
    kwsMedia.createMediaPipeline(function(error, pipeline)
    {
      if(error) return console.error(error);

      // Create pipeline media elements (endpoints & filters)
      PlayerEndpoint.create(pipeline,
      {uri: "https://ci.kurento.com/video/small.webm"},
      function(error, player)
      {
        if(error) return console.error(error);

        // Subscribe to PlayerEndpoint EOS event
        player.on('EndOfStream', function(event)
        {
          console.log("EndOfStream event:", event);
        });

        HttpGetEndpoint.create(pipeline, function(error, httpGet)
        {
          if(error) return console.error(error);

          console.log('httpGet',httpGet);

          // Connect media element between them
          pipeline.connect(player, httpGet, function(error, pipeline)
          {
            if(error) return console.error(error);

            console.log('pipeline',pipeline);

            // Set the video on the video tag
            httpGet.getUrl(function(error, url)
            {
              if(error) return console.error(error);

              videoOutput.src = url;

              console.log(url);

              // Start player
              player.play(function(error)
              {
                if(error) return console.error(error);

                console.log('player.play');
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
});
