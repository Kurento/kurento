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

    kurentoClient.getSingleton(args.ws_uri, function(error, client) {
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


var app = expressWs(express()).app;


/*
 * Definition of global variables.
 */

var master = null;
var viewers = [];

/*
 * Server startup
 */

var asUrl = url.parse(args.as_uri);
var port = asUrl.port;

app.listen(port, function() {
  console.log('Kurento Tutorial started');
  console.log('Open ' + url.format(asUrl) + ' with a WebRTC capable browser');
});

/*
 * Management of WebSocket messages
 */
app.ws('/', function(ws)
{
  console.log('Connection received');

  var rpcBuilder = new RpcBuilder(packer, ws, function(request)
  {
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
        stop()
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


  function onError(error)
  {
    if(error)
    {
      this.reply(error);
      stop();
    }
  }

  function checkMaster(error)
  {
    if(error)
    {
      onError.call(this, error)
      return true
    }

    if(!master)
    {
      this.reply('No active sender now. Become sender or try again later...');
      return true
    }
  }


  function processMaster(request)
  {
    var sdpOffer = request.params[0];

    if(master) return request.reply("Another user is currently acting as sender. Try again later...");

    master = ws;

    getPipeline(function(error, pipeline)
    {
      if(checkMaster.call(request, error)) return;

      pipeline.create('WebRtcEndpoint', function(error, webRtcEndpoint)
      {
        if(checkMaster.call(request, error)) return;

        ws.webRtcEndpoint = webRtcEndpoint;

        addCandidates(webRtcEndpoint)

        webRtcEndpoint.processOffer(sdpOffer, function(error, sdpAnswer)
        {
          if(checkMaster.call(request, error)) return;

          request.reply(null, sdpAnswer);
        });
        webRtcEndpoint.gatherCandidates(onError.bind(request));
      });
    });
  }

  function processViewer(request)
  {
    var sdpOffer = request.params[0];

    if(!master || !master.webRtcEndpoint)
      return callback("No active sender now. Become sender or try again later...");

  //  if(viewers.indexOf(ws) > -1)
  //    return request.reply("You are already viewing in this session. Use a different browser to add additional viewers.")

    viewers.push(ws)

    getPipeline(function(error, pipeline)
    {
      if(checkMaster.call(request, error)) return;

      pipeline.create('WebRtcEndpoint', function(error, webRtcEndpoint)
      {
        if(checkMaster.call(request, error)) return;

        ws.webRtcEndpoint = webRtcEndpoint

        addCandidates(webRtcEndpoint)

        webRtcEndpoint.processOffer(sdpOffer, function(error, sdpAnswer)
        {
          if(checkMaster.call(request, error)) return;

          request.reply(null, sdpAnswer);
        });
        webRtcEndpoint.gatherCandidates(onError);

        master.webRtcEndpoint.connect(webRtcEndpoint, checkMaster.bind(request));
      });
    });
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
    if(master === ws)
    {
      viewers.forEach(function(viewer)
      {
        viewer.send(JSON.stringify(
        {
          jsonrpc: '2.0',
          method: 'stop'
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
    console.log('Connection error');
    stop();
  });

  ws.on('close', function() {
    console.log('Connection closed');
    stop();
  });
});


app.use(express.static(path.join(__dirname, 'static')));
