#!/usr/bin/env node
/*
 * (C) Copyright 2014-2015 Kurento (http://kurento.org/)
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
var expressWs = require('express-ws');

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
 * Definition of global variables.
 */

var users = {};


/*
 * Server startup
 */

var asUrl = url.parse(args.as_uri);
var port = asUrl.port;
var server = app.listen(port, function() {
  console.log('Kurento Tutorial started');
  console.log('Open ' + url.format(asUrl) + ' with a WebRTC capable browser');
});


app.ws('/', function(ws)
{
  console.log('Connection received');

  var rpcBuilder = new RpcBuilder(packer, ws, function(request)
  {
    console.log('Connection received message', request.method, request.params[0]);

    switch(request.method)
    {
      case 'register':
        register(request);
        break;

      case 'call':
        call(request);
        break;

      case 'callResponse':
        callResponse(request);
        break;

      case 'candidate':
        processCandidate(request)
      break;

      case 'stop':
        stop(rpcBuilder);
      break;

      default:
        console.error(request)
        request.reply('Invalid message ' + message);
    }
  });


  var candidatesQueue = []

  rpcBuilder.addCandidates = function(webRtcEndpoint)
  {
    while(candidatesQueue.length)
    {
      var candidate = candidatesQueue.shift()

      webRtcEndpoint.addIceCandidate(candidate)
    }

    var self = this

    webRtcEndpoint.on('OnIceCandidate', function(event) {
      self.encode('candidate', [event.candidate])
    });
  }


  function register(request){
    var name = request.params[0]

    function onError(error)
    {
      if(error)
      {
        console.log("Error processing register:", error);
        request.reply(error)
      }
    }

    if(!name) return onError("empty user name");

    if(users[name]) return onError("already registered");

    rpcBuilder.name = name
    users[name] = rpcBuilder

    request.reply()
  }

  function call(request)
  {
    var to = request.params[0]

    var callee = users[to];
    if(!callee) return request.reply('user ' + to + ' is not registered');

    callee.encode('call', [rpcBuilder.name], function(error)
    {
      if(error) return request.reply(error);

      rpcBuilder.request = request
    });
  }

  function callResponse(request)
  {
    var from     = request.params[0]
    var sdpOffer = request.params[1]

    var caller = users[from];
    if(!caller) return request.reply('unknown from = ' + from);

    rpcBuilder.peer = caller;
    caller.peer     = rpcBuilder;

    var callerRequest = caller.request
    delete caller.request

    function onError(error)
    {
      if(error)
      {
        request.reply(error);
        callerRequest.reply(error)
      }
    }

    kurentoClient.getSingleton(args.ws_uri, function(error, client)
    {
      if(error) return onError(error);

      client.create('MediaPipeline', function(error, pipeline)
      {
        if(error) return onError(error);

        caller.pipeline = pipeline

        function onError(error)
        {
          if(error)
          {
            pipeline.release();
            request.reply(error);
            callerRequest.reply(error)
          }
        }

        pipeline.create('WebRtcEndpoint', function(error, callerWebRtcEndpoint)
        {
          if(error) return onError(error);

          caller.addCandidates(callerWebRtcEndpoint)

          callerWebRtcEndpoint.processOffer(callerRequest.params[1],
            function(error, sdpAnswer)
          {
            if(error) return onError(error)

            callerRequest.reply(null, sdpAnswer);
          })
          callerWebRtcEndpoint.gatherCandidates(onError);

          pipeline.create('WebRtcEndpoint', function(error, webRtcEndpoint)
          {
            if(error) return onError(error);

            rpcBuilder.addCandidates(webRtcEndpoint)

            webRtcEndpoint.processOffer(sdpOffer, function(error, sdpAnswer)
            {
              if(error) return onError(error)

              request.reply(null, sdpAnswer);
            })
            webRtcEndpoint.gatherCandidates(onError);

            client.connect(callerWebRtcEndpoint, webRtcEndpoint,
              callerWebRtcEndpoint, onError);
          });
        });
      });
    })
  }

  function processCandidate(request)
  {
    var candidate = request.params[0];

    candidate = kurentoClient.register.complexTypes.IceCandidate(candidate);

    var webRtcEndpoint = ws.webRtcEndpoint
    if(webRtcEndpoint)
      webRtcEndpoint.addIceCandidate(candidate)
    else
      candidatesQueue.push(candidate)
  }


  ws.on('error', function(error)
  {
    console.log('Connection error');
    stop(rpcBuilder);
  });

  ws.on('close', function()
  {
    console.log('Connection closed');
    stop(rpcBuilder);
  });
});


function stop(rpcBuilder)
{
  delete users[rpcBuilder.name]

  if(rpcBuilder.pipeline)
  {
    rpcBuilder.pipeline.release()
    delete rpcBuilder.pipeline
  }

  if(rpcBuilder.peer)
  {
    rpcBuilder.peer.encode('stop', ['remote user hanged out'])

    delete rpcBuilder.peer.pipeline
    delete rpcBuilder.peer.peer
    delete rpcBuilder.peer
  }
}


app.use(express.static(path.join(__dirname, 'static')));
