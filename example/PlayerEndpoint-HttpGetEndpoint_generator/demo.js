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

const ws_uri = 'ws://demo01.kurento.org:8888/thrift/ws/websocket';

const URL_SMALL = "http://files.kurento.org/video/small.webm";


window.addEventListener('load', function()
{
  var videoOutput = document.getElementById('videoOutput');

  function* gen()
  {
    var kwsMedia = yield KwsMedia(ws_uri);

    // Create pipeline media elements (endpoints & filters)
    var pipeline = yield kwsMedia.create('MediaPipeline');
    var player   = yield pipeline.create('PlayerEndpoint', {uri: URL_SMALL});

    // Subscribe to PlayerEndpoint EOS event
    player.on('EndOfStream', function(event)
    {
      console.log('EndOfStream event:', event);
    });

    var httpGet = yield pipeline.create('HttpGetEndpoint');

    // Connect media element between them
    player.connect(httpGet);

    // Set the video on the video tag
    videoOutput.src = yield httpGet.getUrl();

    // Start player
    player.play();
  };

  co(gen)(function(error)
  {
    if(error) console.error(error);
  });
});
