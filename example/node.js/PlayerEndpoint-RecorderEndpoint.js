var KwsMedia = require('../..')


const ws_uri = 'ws://kms01.kurento.org:8080/thrift/ws/websocket';

const URL_SMALL = "https://ci.kurento.com/video/small.webm";


function onerror(error)
{
  console.error(error);

  kwsMedia.close();
};


var kwsMedia = KwsMedia(ws_uri, function(kwsMedia)
{
  // Create pipeline
  kwsMedia.create('MediaPipeline', function(error, pipeline)
  {
    if(error) return onerror(error);

    console.log('pipeline');

    // Create pipeline media elements (endpoints & filters)
    pipeline.create('PlayerEndpoint', {uri: URL_SMALL}, function(error, player)
    {
      if(error) return onerror(error);

      console.log('player');

      // Subscribe to PlayerEndpoint EOS event
      player.on('EndOfStream', function(event)
      {
        console.log("Player EndOfStream");

        pipeline.release();
        kwsMedia.close();
      });

      pipeline.create('RecorderEndpoint', function(error, recorder)
      {
        if(error) return onerror(error);

        console.log('recorder');

        // Connect media element between them
        player.connect(recorder, function(error)
        {
          if(error) return onerror(error);

          recorder.getUrl(function(error, url)
          {
            if(error) return onerror(error);

            console.log(url);

            // Start player
            recorder.record(function(error)
            {
              if(error) return onerror(error);

              console.log('recorder.record');
            });
          });
        });
      });
    });
  });
},
onerror);
