/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */

const ws_uri = 'ws://kms01.kurento.org:8080/thrift/ws/websocket';


function onerror(error)
{
  console.error(error);
};


window.addEventListener('load', function()
{
  var videoOutput = document.getElementById("videoOutput");

  KwsMedia(ws_uri, function(kwsMedia)
  {
    // Create pipeline
    kwsMedia.create('MediaPipeline', function(error, pipeline)
    {
      if(error) return onerror(error);

      // Create pipeline media elements (endpoints & filters)
      pipeline.create('PlayerEndpoint',
      {uri: "https://ci.kurento.com/video/small.webm"},
      function(error, player)
      {
        if(error) return console.error(error);

        // Subscribe to PlayerEndpoint EOS event
        player.on('EndOfStream', function(event)
        {
          console.log("EndOfStream event:", event);
        });

        pipeline.create('HttpGetEndpoint', function(error, httpGet)
        {
          if(error) return onerror(error);

          console.log('httpGet',httpGet);

          // Connect media element between them
          player.connect(httpGet, function(error, pipeline)
          {
            if(error) return onerror(error);

            console.log('pipeline',pipeline);

            // Set the video on the video tag
            httpGet.getUrl(function(error, url)
            {
              if(error) return onerror(error);

              videoOutput.src = url;

              console.log(url);

              // Start player
              player.play(function(error)
              {
                if(error) return onerror(error);

                console.log('player.play');
              });
            });
          });
        });
      });
    });
  },
  onerror);
});
