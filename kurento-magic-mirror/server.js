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


/*
 * Server startup
 */

var asUrl = url.parse(args.as_uri);
var port = asUrl.port;
asUrl = url.format(asUrl)

var server = app.listen(port, function() {
  console.log('Kurento Tutorial started');
  console.log('Open ' + asUrl + ' with a WebRTC capable browser');
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
      case 'offer':
        processOffer(request)
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


  var candidatesQueue = []

  function processOffer(request) {
    var sdpOffer = request.params[0];

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

        pipeline.create('WebRtcEndpoint', function(error, _webRtcEndpoint) {
          if (error) return onError(error);

          webRtcEndpoint = _webRtcEndpoint

          while(candidatesQueue.length)
          {
            var candidate = candidatesQueue.shift()

            webRtcEndpoint.addIceCandidate(candidate)
          }

          webRtcEndpoint.on('OnIceCandidate', function(event) {
            rpcBuilder.encode('candidate', [event.candidate])
          });

          webRtcEndpoint.processOffer(sdpOffer, function(error, sdpAnswer) {
            if (error) return onError(error)

            request.reply(null, sdpAnswer);
          });
          webRtcEndpoint.gatherCandidates(onError);

          pipeline.create('FaceOverlayFilter', function(error, faceOverlayFilter) {
            if (error) return onError(error);

            faceOverlayFilter.setOverlayedImage(asUrl+"img/mario-wings.png",
              -0.35, -1.2, 1.6, 1.6, onError);

            client.connect(webRtcEndpoint, faceOverlayFilter, webRtcEndpoint,
              onError);
          });
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

  function stop() {
    if(pipeline) pipeline.release();
  }


  ws.on('error', function(error) {
    console.error(error);
    stop();
  });

  ws.on('close', function() {
    console.log('Connection closed');
    stop();
  });
});

app.use(express.static(path.join(__dirname, 'static')));
