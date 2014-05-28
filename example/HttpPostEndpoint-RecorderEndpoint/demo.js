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


getUserMedia({'audio': true, 'video': true}, function(stream)
{
  var videoInput = document.getElementById("videoInput");
      videoInput.src = URL.createObjectURL(stream);


  KwsMedia(ws_uri, function(kwsMedia)
  {
    // Create pipeline
    kwsMedia.createMediaPipeline(function(error, pipeline)
    {
      if(error) return onerror(error);

      // Create pipeline media elements (endpoints & filters)
      HttpPostEndpoint.create(pipeline, function(httpPos)
      {
        if(error) return onerror(error);

        RecorderEndpoint.create(pipeline, function(error, recorder)
        {
          if(error) return onerror(error);

          // Connect media element between them
          pipeline.connect(httpPost, recorder,
          function(error, pipeline)
          {
            if(error) return onerror(error);

            console.log(pipeline);
          });

          httpPost.getUrl(function(error, url)
          {
            if(error) return onerror(error);

            // Set the video on the video tag
            var xhr = new XmlHttpRequest();
                xhr.open('post', url);
                xhr.send(stream);

            console.log(url);

            // Start recorder
            recorder.record(function(error, result)
            {
              if(error) return onerror(error);

              console.log(result);
            });
          });

        });
      });
    });
  },
  onerror);
},
onerror);
