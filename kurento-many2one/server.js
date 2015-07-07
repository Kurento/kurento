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

var path = require('path');

var express  = require('express');
var minimist = require('minimist');
var ws       = require('ws');

var kurento = require('kurento-client');


var argv = minimist(process.argv.slice(2),
{
  default:
  {
    ws_uri: "ws://localhost:8888/kurento"
  }
});


var app = express();
app.set('port', process.env.PORT || 8080);


/*
 * Definition of global variables.
 */

var idCounter = 0;
var master = null;
var pipeline = null;
var viewers = {};
var kurentoClient = null;

function nextUniqueId() {
  idCounter++;
  return idCounter.toString();
}
/*
 * Server startup
 */

var port = app.get('port');
var server = app.listen(port, function() {
  console.log('Express server started ');
  console.log('Connect to http://<host_name>:' + port + '/');
});

var wss = new ws.Server({
  server : server,
  path : '/call'
});

/*
 * Management of WebSocket messages
 */
wss.on('connection', function(ws) {

  var sessionId = nextUniqueId();

  console.log('Connection received with sessionId ' + sessionId);

  ws.on('error', function(error) {
    console.log('Connection ' + sessionId + ' error');
    stop(sessionId);
  });

  ws.on('close', function() {
    console.log('Connection ' + sessionId + ' closed');
    stop(sessionId);
  });

  ws.on('message', function(_message) {
    var message = JSON.parse(_message);
    console.log('Connection ' + sessionId + ' received message ', message);

    switch (message.id) {
    case 'master':
      startMaster(sessionId, message.sdpOffer,
        function(error, sdpAnswer) {
          if (error) {
            return ws.send(JSON.stringify({
              id : 'masterResponse',
              response : 'rejected',
              message : error
            }));
          }
          ws.send(JSON.stringify({
            id : 'masterResponse',
            response : 'accepted',
            sdpAnswer : sdpAnswer
          }));
        });
      break;

    case 'viewer':
      startViewer(sessionId, message.sdpOffer, ws, function(error,
          sdpAnswer) {
        if (error) {
          return ws.send(JSON.stringify({
            id : 'viewerResponse',
            response : 'rejected',
            message : error
          }));
        }

        ws.send(JSON.stringify({
          id : 'viewerResponse',
          response : 'accepted',
          sdpAnswer : sdpAnswer
        }));
      });
      break;

    case 'newViewer':
      startViewer(sessionId, message.sdpOffer, ws, function(error,
          sdpAnswer) {
        if (error) {
          return ws.send(JSON.stringify({
            id : 'newViewerResponse',
            response : 'rejected',
            message : error
          }));
        }

        ws.send(JSON.stringify({
          id : 'newViewerResponse',
          response : 'accepted',
          sdpAnswer : sdpAnswer
        }));
      });
      break;

    case 'stop':
      stop(sessionId);
      break;

    default:
      ws.send(JSON.stringify({
        id : 'error',
        message : 'Invalid message ' + message
      }));
      break;
    }
  });
});

/*
 * Definition of functions
 */

// Recover kurentoClient for the first time.
function getKurentoClient(callback) {
  if (kurentoClient !== null) {
    return callback(null, kurentoClient);
  }

  kurento(argv.ws_uri, function(error, _kurentoClient) {
    if (error) {
      console.log("Coult not find media server at address " + argv.ws_uri);
      return callback("Could not find media server at address" + argv.ws_uri
          + ". Exiting with error " + error);
    }

    kurentoClient = _kurentoClient;
    callback(null, kurentoClient);
  });
}

function startMaster(id, sdp, callback) {
  if (master !== null) {
    return callback("Another user is currently acting as sender. Try again later ...");
  }

  master = {
    id : id,
    webRtcEndpoint : null
  };

  getKurentoClient(function(error, kurentoClient) {
    if (error) {
      stop(id);
      return callback(error);
    }

    if (master === null) {
      return callback('Request was cancelled by the user. You will not be sending any longer');
    }

    kurentoClient.create('MediaPipeline', function(error, _pipeline) {
      if (error) {
        return callback(error);
      }

      if (master === null) {
        return callback('Request was cancelled by the user. You will not be sending any longer');
      }

      _pipeline.create('WebRtcEndpoint', function(error, webRtcEndpoint) {
        if (error) {
          stop(id);
          return callback(error);
        }

        if (master === null) {
          return callback('Request was cancelled by the user. You will not be sending any longer');
        }

        master.webRtcEndpoint = webRtcEndpoint;

        webRtcEndpoint.processOffer(sdp, function(error, sdpAnswer) {
          if (error) {
            stop(id)
            return callback(error);
          }

          if (master === null) {
            return callback('Request was cancelled by the user. You will not be sending any longer');
          }

          if (pipeline !== null) {
            replace();
          }
          pipeline = _pipeline;

          callback( null, sdpAnswer);
        });
      });
    });
  });
}

function startViewer(id, sdp, ws, callback)
{
  if(!master || !master.webRtcEndpoint)
    return callback("No active sender now. Become sender or . Try again later ...");

  pipeline.create('WebRtcEndpoint', function(error, webRtcEndpoint)
  {
    if(error) return callback(error);

    if(!master)
    {
      stop(id);
      return callback("No active sender now. Become sender or . Try again later ...");
    }

    webRtcEndpoint.processOffer(sdp, function(error, sdpAnswer)
    {
      if(error)
      {
        stop(id);
        return callback(error);
      }

      if(!master)
      {
        stop(id);
        return callback("No active sender now. Become sender or . Try again later ...");
      }

      master.webRtcEndpoint.connect(webRtcEndpoint, function(error)
      {
        if(error)
        {
          stop(id);
          return callback(error);
        }

        if(!master)
        {
          stop(id);
          return callback("No active sender now. Become sender or . Try again later ...");
        }

        callback(null, sdpAnswer);

//        // Replace viewer
//        if(viewers[id])
//          stop(id);

        viewers[id] =
        {
          id : id,
          ws : ws,
          webRtcEndpoint : webRtcEndpoint
        };
      });
    });
  });
}

function removeReceiver(id) {
  if (!receivers[id]) {
    return;
  }
  var receiver = receivers[id];
  receiver.webRtcEndpoint.release();
  delete receiver[id];
}

function removeSender() {
  if (sender === null) {
    return;
  }

  for ( var ix in receivers) {
    removeReceiver(ix);
  }

  sender.webRtcEndpoint.release();
  sender = null;
}

function stop(id, ws) {
  if (master !== null && master.id == id) {
    for ( var ix in viewers) {
      var viewer = viewers[ix];
      if (viewer.ws) {
        viewer.ws.send(JSON.stringify({
          id : 'stopCommunication'
        }));
      }
    }
    viewers = {};
    pipeline.release();
    pipeline = null;
    master = null;
  } else if (viewers[id]) {
    var viewer = viewers[id];
    if (viewer.webRtcEndpoint)
      viewer.webRtcEndpoint.release();
    delete viewers[id];
  }
}

app.use(express.static(path.join(__dirname, 'static')));
