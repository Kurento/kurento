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
  var kwsMedia = KwsMedia(ws_uri);

  kwsMedia
  .then(function()
  {
    // Create pipeline
    return kwsMedia.create('MediaPipeline');
  })
  .then(function(pipeline)
  {
    // Create pipeline media elements (endpoints & filters)
    return Promise.all(
    [
      pipeline.create('PlayerEndpoint',
      {uri: "https://ci.kurento.com/video/small.webm"})
      .then(function(player)
      {
        // Subscribe to PlayerEndpoint EOS event
        player.on('EndOfStream', function(event)
        {
          console.log("EndOfStream event:", event);
        });

        return player;
      }),
      pipeline.create('HttpGetEndpoint')
    ])
  })
  .then(function(values)
  {
    var player  = values[0];
    var httpGet = values[1];

    console.log('httpGet',httpGet);

    return Promise.all(
    [
      // Connect media element between them
      player.connect(httpGet),
      httpGet.getUrl()
      .then(function(url)
      {
        var videoOutput = document.getElementById("videoOutput");

        videoOutput.src = url;

        console.log(url);
      })
    ])
    .then(function()
    {
      return player;
    })
  })
  .then(function(player)
  {
    // Start player
    return player.play();
  })
  .then(function()
  {
    console.log('Start.playing');
  })
  .catch(onerror);
});
