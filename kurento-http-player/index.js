/*
* (C) Copyright 2014-2015 Kurento (http://kurento.org/)
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

function getopts(args, opts)
{
  var result = opts.default || {};
  args.replace(
      new RegExp("([^?=&]+)(=([^&]*))?", "g"),
      function($0, $1, $2, $3) { result[$1] = $3; });

  return result;
};

var args = getopts(location.search,
{
  default:
  {
    ws_uri: 'ws://' + location.hostname + ':8888/kurento',
    as_uri: 'http://' + location.host
  }
});

const file_uri = args.as_uri+'/video/fiwarecut.mp4';
const hat_uri  = args.as_uri+'/img/mario-wings.png';


function onError(error)
{
  if(error) console.log(error);
}


window.addEventListener("load", function(event)
{
  var videoOutput = document.getElementById('videoOutput');

  var playButton = document.getElementById("playButton");
  var stopButton = document.getElementById("stopButton");

  playButton.addEventListener("click", function()
  {
    console.log("Strarting video playing...");

    kurentoClient(args.ws_uri, function(error, client)
    {
      if(error) return onError(error);

      client.create('MediaPipeline', function(error, pipeline)
      {
        if(error) return onError(error);

        function release()
        {
          pipeline.release();
          videoOutput.src = "";
        }

        function onError(error)
        {
          if(error)
          {
            console.log(error);
            release()
          }
        }

        stopButton.addEventListener("click", release);

        var options = {uri: file_uri}

        pipeline.create('PlayerEndpoint', options, function(error, playerEndpoint)
        {
          if(error) return onError(error);

          playerEndpoint.on('EndOfStream', release);

          pipeline.create('FaceOverlayFilter', function(error, filter)
          {
            if(error) return onError(error);

            console.log('Got FaceOverlayFilter');

            filter.setOverlayedImage(hat_uri, -0.2, -1.1, 1.6, 1.6, onError);

            pipeline.create('HttpGetEndpoint', function(error, httpGetEndpoint)
            {
              if(error) return onError(error);

              httpGetEndpoint.getUrl(function(error, url)
              {
                if(error) return onError(error);

                videoOutput.src = url;
              });

              client.connect(playerEndpoint, filter, httpGetEndpoint, function(error)
              {
                if(error) return onError(error);

                playerEndpoint.play(function(error)
                {
                  if(error) return onError(error);

                  console.log('Playing...');
                });
              });
            });
          });
        });
      });
    });
  });
});
