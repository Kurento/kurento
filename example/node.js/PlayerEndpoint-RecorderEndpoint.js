var KwsMedia = require('../..')


const ws_uri = 'ws://192.168.0.110:7788/thrift/ws/websocket';

const URL_SMALL = "https://ci.kurento.com/video/small.webm";


function onerror(error)
{
  console.error(error);
};


KwsMedia(ws_uri, function(kwsMedia)
{
  // Create pipeline
  kwsMedia.create('MediaPipeline', function(error, pipeline)
  {
    if(error) return console.error(error);

    // Create pipeline media elements (endpoints & filters)
    pipeline.create('PlayerEndpoint', {uri: URL_SMALL}, function(error, player)
    {
      if(error) return console.error(error);

      // Subscribe to PlayerEndpoint EOS event
      player.on('EndOfStream', function(event)
      {
        console.log("EndOfStream event:", event);
      });

      pipeline.create('RecorderEndpoint', function(error, recorder)
      {
        if(error) return console.error(error);

        console.log('recorder', recorder);

        // Connect media element between them
        player.connect(recorder, function(error)
        {
          if(error) return console.error(error);

          recorder.getUrl(function(error, url)
          {
            if(error) return console.error(error);

            console.log(url);

            // Start player
            recorder.record(function(error)
            {
              if(error) return console.error(error);

              console.log('recorder.record');
            });
          });
        });
      });
    });
  });
},
onerror);
