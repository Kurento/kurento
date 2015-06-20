#!/usr/bin/env node
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

var path = require('path');
var url  = require('url');

var minimist = require('minimist');

var express   = require('express');
var expressWs = require('express-ws');

var kurentoClient = require('kurento-client')
var RpcBuilder    = require('kurento-jsonrpc');

const packer = RpcBuilder.packers.JsonRPC;



var args = minimist(process.argv.slice(2),
{
  default:
  {
    as_uri: "http://localhost:8080/",
    ws_uri: "ws://localhost:8888/kurento"
  }
});

var app = expressWs(express()).app;


kurentoClient.register('kurento-module-crowddetector')

const RegionOfInterest       = kurentoClient.register.complexTypes.RegionOfInterest
const RegionOfInterestConfig = kurentoClient.register.complexTypes.RegionOfInterestConfig
const RelativePoint          = kurentoClient.register.complexTypes.RelativePoint

app.set('port', process.env.PORT || 8080);


/*
 * Server startup
 */

var asUrl = url.parse(args.as_uri);
var port = asUrl.port;
var server = app.listen(port, function() {
  console.log('Kurento Tutorial started');
  console.log('Open ' + url.format(asUrl) + ' with a WebRTC capable browser');
});

/*
 * Management of WebSocket messages
 */
app.ws('/', function(ws)
{
  var pipeline
  var webRtcEndpoint

  var rpcBuilder = new RpcBuilder(packer, ws, function(request)
  {
    switch(request.method)
    {
      case 'start':
        start(request)
      break;

      case 'candidate':
        processCandidate(request)
      break;

      case 'stop':
        stop();
      break;

      default:
        console.error(request)
        request.reply('Invalid message')
    }
  });


  function retransmitEvent(object, event)
  {
    object.on(event, rpcBuilder.encode.bind(rpcBuilder, event));
  }

  var candidatesQueue = []

  function start(request)
  {
    var sdpOffer = request.params[0];

    // Check if session is already transmitting
    if (pipeline)
      return request.reply("Close current session before starting a new one or use another browser to open a tutorial.")

    kurentoClient.getSingleton(args.ws_uri, function(error, client) {
      if (error) return request.reply(error);

      client.create('MediaPipeline', function(error, _pipeline) {
        if (error) return request.reply(error);

        pipeline = _pipeline

        function onError(error)
        {
          if(error)
          {
            pipeline.release();
            request.reply(error);
          }
        }

        createMediaElements(pipeline, function(error, _webRtcEndpoint, crowdDetector)
        {
          if(error) return onError(error)

          webRtcEndpoint = _webRtcEndpoint

          while(candidatesQueue.length)
          {
            var candidate = candidatesQueue.shift()

            webRtcEndpoint.addIceCandidate(candidate)
          }

          webRtcEndpoint.on('OnIceCandidate', function(event) {
            rpcBuilder.encode('candidate', [event.candidate])
          });

          webRtcEndpoint.processOffer(sdpOffer, function(error, sdpAnswer)
          {
            if (error) return onError(error)

            return request.reply(null, sdpAnswer);
          });
          webRtcEndpoint.gatherCandidates(onError);

          retransmitEvent(crowdDetector, 'CrowdDetectorDirection')
          retransmitEvent(crowdDetector, 'CrowdDetectorFluidity')
          retransmitEvent(crowdDetector, 'CrowdDetectorOccupancy')

          client.connect(webRtcEndpoint, crowdDetector, webRtcEndpoint, onError);
        });
      });
    });
  }

  function processCandidate(request)
  {
    var candidate = request.params[0];

    candidate = kurentoClient.register.complexTypes.IceCandidate(candidate);

    if(webRtcEndpoint)
      webRtcEndpoint.addIceCandidate(candidate)
    else
      candidatesQueue.push(candidate)
  }

  function stop()
  {
    if(pipeline)
    {
      pipeline.release();
      pipeline = null;
    }
  }


  ws.on('error', function(error) {
    console.log('Connection error');
    stop();
  });

  ws.on('close', function() {
    console.log('Connection closed');
    stop();
  });
});


/*
 * Definition of functions
 */

function createMediaElements(pipeline, callback)
{
  pipeline.create('WebRtcEndpoint', function(error, webRtcEndpoint)
  {
    if(error) return callback(error);

    var options =
    {
      rois:
      [
        RegionOfInterest({
          id: 'roi1',
          points:
          [
            RelativePoint({x: 0  , y: 0  }),
            RelativePoint({x: 0.5, y: 0  }),
            RelativePoint({x: 0.5, y: 0.5}),
            RelativePoint({x: 0  , y: 0.5})
          ],
          regionOfInterestConfig: RegionOfInterestConfig({
            occupancyLevelMin: 10,
            occupancyLevelMed: 35,
            occupancyLevelMax: 65,
            occupancyNumFramesToEvent: 5,
            fluidityLevelMin: 10,
            fluidityLevelMed: 35,
            fluidityLevelMax: 65,
            fluidityNumFramesToEvent: 5,
            sendOpticalFlowEvent: false,
            opticalFlowNumFramesToEvent: 3,
            opticalFlowNumFramesToReset: 3,
            opticalFlowAngleOffset: 0
          })
        })
      ]
    }

    pipeline.create('CrowdDetectorFilter', options, function(error, crowdDetector)
    {
      if(error) return callback(error);

      return callback(null, webRtcEndpoint, crowdDetector);
    });
  });
}


app.use(express.static(path.join(__dirname, 'static')));
