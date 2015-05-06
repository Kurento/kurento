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


// Recover kurentoClient for the first time.
var getKurentoClient = (function()
{
  var client = null;

  function disconnect()
  {
    client = null
  }

  return function(callback)
  {
    if(client) return callback(null, client);

    kurentoClient(argv.ws_uri, function(error, _client) {
      if(error) return callback(error);

      client = _client;
      client.on('disconnect', disconnect)

      callback(null, client);
    });
  }
})()


var getPipeline = (function()
{
  var pipeline = null;

  function release()
  {
    pipeline = null
  }

  return function(callback)
  {
    if(pipeline) return callback(null, pipeline);

    getKurentoClient(function(error, client) {
      if(error) return callback(error);

      client.create('MediaPipeline', function(error, _pipeline) {
        if(error) return callback(error);

        pipeline = _pipeline;
        pipeline.on('release', release)

        callback(null, pipeline);
      });
    });
  }
})()


var argv = minimist(process.argv.slice(2),
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

var master = null;
var viewers = [];

/*
 * Server startup
 */

var asUrl = url.parse(argv.as_uri);
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
  var rpcBuilder = new RpcBuilder(packer, ws, function(request)
  {
    console.log('Connection received message', request.method, request.params[0]);

    switch(request.method)
    {
      case 'master':
        processMaster(request)
      break;

      case 'viewer':
        processViewer(request)
      break;

      case 'candidate':
        processCandidate(request)
      break;

      case 'stop':
        stop();
      break;

      default:
        console.error(request)
        request.reply('Invalid message ' + message);
    }
  });


  var candidatesQueue = []

  function addCandidates(webRtcEndpoint)
  {
    while(candidatesQueue.length)
    {
      var candidate = candidatesQueue.shift()

      webRtcEndpoint.addIceCandidate(candidate)
    }

    webRtcEndpoint.on('OnIceCandidate', function(event) {
      rpcBuilder.encode('candidate', [event.candidate])
    });
  }


  function processMaster(request)
  {
    var sdpOffer = request.params[0];

    if(master) return request.reply("Another user is currently acting as sender. Try again later...");

    master = ws;
    console.log('processMaster', master !== undefined)

    function onError(error)
    {
      if(error)
      {
        console.trace(error)
        request.reply(error);
        stop();
      }
    }

    function checkMaster(error)
    {
      if(error)
      {
        onError(error)
        return true
      }

      if(!master)
      {
        request.reply('Request was cancelled by the user. You will not be sending any longer');
        return true
      }
    }

    getPipeline(function(error, pipeline) {
      if(checkMaster(error)) return

      pipeline.create('WebRtcEndpoint', function(error, webRtcEndpoint) {
        if(checkMaster(error)) return

        ws.webRtcEndpoint = webRtcEndpoint;

        addCandidates(webRtcEndpoint)

        webRtcEndpoint.processOffer(sdpOffer, function(error, sdpAnswer) {
          if(checkMaster(error)) return

          request.reply(null, sdpAnswer);
        });
        webRtcEndpoint.gatherCandidates(onError);
      });
    });
  }

  function processViewer(request)
  {
    var sdpOffer = request.params[0];

    if(!master || !master.webRtcEndpoint)
      return request.reply("No active sender now. Become sender or try again later");

    if(viewers.indexOf(ws) > -1)
      return request.reply("You are already viewing in this session. Use a different browser to add additional viewers.")

    viewers.push(ws);

    function onError(error)
    {
      if(error)
      {
        console.trace(error)
        request.reply(error);
        stop();
      }
    }

    function checkMaster(error)
    {
      if(error)
      {
        onError(error)
        return true
      }

      if(!master)
      {
        request.reply('No active sender now. Become sender or try again later...');
        return true
      }
    }

    getPipeline(function(error, pipeline) {
      pipeline.create('WebRtcEndpoint', function(error, webRtcEndpoint) {
        if(checkMaster(error)) return

        ws.webRtcEndpoint = webRtcEndpoint

        addCandidates(webRtcEndpoint)

        webRtcEndpoint.processOffer(sdpOffer, function(error, sdpAnswer) {
          if(checkMaster(error)) return

          master.webRtcEndpoint.connect(webRtcEndpoint, function(error) {
            if(checkMaster(error)) return

            request.reply(null, sdpAnswer);
          });
        });
        webRtcEndpoint.gatherCandidates(onError);
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


  function stop() {
    if (master === ws) {
      viewers.forEach(function(viewer)
      {
        viewer.send(JSON.stringify(
        {
          jsonrpc: '2.0',
          method: 'stopCommunication'
        }));
      })

      getPipeline(function(error, pipeline)
      {
        if(error) return console.error(error)

        pipeline.release()
      })

      master = null;
      viewers = [];
    }
    else
    {
      var index = viewers.indexOf(ws)

      if(index > -1)
      {
        var viewer = viewers[index];

        if(viewer.webRtcEndpoint)
          viewer.webRtcEndpoint.release();

        viewers.splice(index, 1);
      }
    }
  }


  ws.on('error', function(error) {
    console.log('Connection error:',error);
    stop();
  });

  ws.on('close', function() {
    console.log('Connection closed');
    stop();
  });
});


app.use(express.static(path.join(__dirname, 'static')));
