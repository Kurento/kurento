var fs   = require('fs');
var http = require('http');

var KwsMedia = require('../..')


const ws_uri = 'ws://demo01.kurento.org:8080/thrift/ws/websocket';

const URL_SMALL = "http://files.kurento.org/video/small.webm";


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
    if(error) return console.error(error);

    console.log('pipeline');

    // Create pipeline media elements (endpoints & filters)
    pipeline.create('PlayerEndpoint', {uri: URL_SMALL}, function(error, player)
    {
      if(error) return console.error(error);

      console.log('player');

      // Subscribe to PlayerEndpoint EOS event
      player.on('EndOfStream', function(event)
      {
        console.log("Player EndOfStream");

        pipeline.release();
        kwsMedia.close();
      });

      pipeline.create('HttpGetEndpoint', function(error, httpGet)
      {
        if(error) return console.error(error);

        console.log('httpGet');

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
                console.log("Got response");

                var file = fs.createWriteStream("file.webm");

                response.pipe(file).on('end', function()
                {
                  kwsMedia.close();
                });
              }).on('error', onerror);
            });
          });
        });

//        // Subscribe to HttpGetEndpoint EOS event
//        httpGet.on('EndOfStream', function(event)
//        {
//          console.log("httpGet EndOfStream");
//        });
      });
    });
  });
},
onerror);
