#!/usr/bin/env node
/*
 * (C) Copyright 2013-2015 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License (LGPL)
 * version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

var path = require('path');
var url  = require('url');

var minimist = require('minimist');

var express   = require('express');
var expressWs = require('express-ws')

var kurentoClient = require('kurento-client');
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

var server = app.listen(port, function() {
  console.log('Kurento Tutorial started');
  console.log('Open ' + url.format(asUrl) + ' with a WebRTC capable browser');
});

/*
 * Definition of functions
 */
function sendError(res, code, error) {
  console.error("Error:",error);

  res.type('text/plain');
  res.status(code);
  res.send(error);
}

/*
 * Code for processing WebSocket request from client
 */
app.ws('/', function(ws) {
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

      default:
        console.error(request)
    }
  });


  /*
   * Code for recovering a media pipeline.
   */
  function getPipeline(callback) {
    if (pipeline) return callback(null, pipeline);

    kurentoClient(args.ws_uri, function(error, client) {
      if (error) return callback(error);

      client.create('MediaPipeline', function(error, _pipeline) {
        if (error) return callback(error);

        pipeline = _pipeline;
        return callback(null, pipeline);
      });
    });
  }


  var candidatesQueue = []

  function processOffer(request)
  {
    var sdpOffer = request.params[0];

    console.log("Request received from client with sdpOffer =",sdpOffer);

    getPipeline(function(error, pipeline) {
      if (error) return request.reply(error);

      console.log("Creating WebRtcEndpoint");

      pipeline.create('WebRtcEndpoint', function(error, _webRtcEndpoint) {
        if (error) return request.reply(error);

        webRtcEndpoint = _webRtcEndpoint

        while(candidatesQueue.length)
        {
          var candidate = candidatesQueue.shift()

          webRtcEndpoint.addIceCandidate(candidate)
        }

        webRtcEndpoint.on('OnIceCandidate', function(event) {
          rpcBuilder.encode('candidate', [event.candidate])
        });

        function onError(error)
        {
          if(error)
          {
            webRtcEndpoint.release();
            request.reply(error);
          }
        }

        console.log("Processing sdpOffer at server and generating sdpAnswer");

        webRtcEndpoint.processOffer(sdpOffer, function(error, sdpAnswer) {
          if (error) return onError(error)

          console.log("Sending sdpAnswer to client:",sdpAnswer);
          request.reply(null, sdpAnswer);
        });

        webRtcEndpoint.gatherCandidates(onError);
        webRtcEndpoint.connect(webRtcEndpoint, onError);
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
});

app.use(express.static(path.join(__dirname, 'static')));
