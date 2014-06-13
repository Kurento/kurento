var PlayerEndpoint  = KwsMedia.endpoints.PlayerEndpoint;
var HttpGetEndpoint = KwsMedia.endpoints.HttpGetEndpoint;


const ws_uri = 'ws://193.147.51.35:7788/thrift/ws/websocket';

const URL_SMALL = "http://files.kurento.org/video/small.webm";


/**
 * Based on http://css-tricks.com/snippets/javascript/get-url-variables/
 */
function getQueryVariable(variable)
{
  var query = window.location.search.substring(1);
  var vars = query.split("&");

  for(var i=0, param; param=vars[i]; i++)
  {
    var pair = param.split("=");
    if(pair[0] == variable)
    {
      param = pair[1];
      if(param != undefined) return param;
      return true;
    }
  }

  return;
}

window.addEventListener('load', function()
{
  var videoOutput = document.getElementById("videoOutput");

  var options =
  {
    access_token: getQueryVariable('access_token')
  };

  KwsMedia(ws_uri, options, function(kwsMedia)
  {
    // Create pipeline
    kwsMedia.createMediaPipeline(function(error, pipeline)
    {
      if(error) return console.error(error);

      // Create pipeline media elements (endpoints & filters)
      PlayerEndpoint.create(pipeline, {uri: URL_SMALL}, function(error, player)
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
