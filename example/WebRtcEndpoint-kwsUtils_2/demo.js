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

const ws_uri = 'ws://demo01.kurento.org:8080/thrift/ws/websocket';


function onerror(error)
{
  console.error(error);
};


window.addEventListener('load', function()
{
  var videoInput  = document.getElementById("videoInput");
  var videoOutput = document.getElementById("videoOutput");

  var webRtcPeer = kwsUtils.WebRtcPeer.startSendRecv(videoInput, videoOutput,
  function(offer)
  {
    console.log('offer+candidates', offer);

    KwsMedia(ws_uri, function(kwsMedia)
    {
      // Create pipeline
      kwsMedia.create('MediaPipeline', function(error, pipeline)
      {
        if(error) return onerror(error);

        // Create pipeline media elements
        pipeline.create('WebRtcEndpoint', function(error, webRtc)
        {
          if(error) return onerror(error);

          // Connect the pipeline to the WebRtcPeer client
          webRtc.processOffer(offer, function(error, answer)
          {
            if(error) return onerror(error);

            console.log('answer', answer);

            webRtcPeer.processSdpAnswer(answer);
          });

          // loopback
          webRtc.connect(webRtc, function(error)
          {
            if(error) return onerror(error);

            console.log('loopback established');
          });
        });
      });
    });
  },
  onerror);
});
