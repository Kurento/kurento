var fs   = require('fs');
var http = require('http');

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
  kwsMedia.create('MediaPipeline' function(error, pipeline)
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

      pipeline.create('HttpGetEndpoint', function(error, httpGet)
      {
        if(error) return console.error(error);

        console.log('httpGet',httpGet);

        // Connect media element between them
        player.connect(httpGet, function(error)
        {
          if(error) return console.error(error);

          httpGet.getUrl(function(error, url)
          {
            if(error) return console.error(error);

            console.log(url);

            // Start player
            player.play(function(error)
            {
              if(error) return console.error(error);

              console.log('player.play');

              http.get(url, function(response)
              {
                console.log("Got response", response);

                var file = fs.createWriteStream("file.webm");

                response.pipe(file).on('end', function()
                {
                  kwsMedia.close();
                });
              }).on('error', function(error)
              {
                console.log("Got error: " + error.message);
              });
            });
          });
        });
      });
    });
  });
},
onerror);
