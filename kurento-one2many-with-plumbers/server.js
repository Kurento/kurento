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
var express = require('express');
var ws = require('ws');
var minimist = require('minimist');
var url = require('url');
var kurento = require('kurento-client');
kurento.register(require('kurento-module-plumberendpoint'));

var argv = minimist(process.argv.slice(2),
{
  default:
  {
    as_uri: "http://localhost:8080/",
    ws_uri_1: "ws://localhost:8888/kurento",
    ws_uri_2: "ws://localhost:8888/kurento",
    ws_uri_3: "ws://localhost:8888/kurento"
  }
});

var app = express();

/*
 * Set here the addresses of your mediaserver:
 */
const uries = [
  argv.ws_uri_1,
  argv.ws_uri_2,
  argv.ws_uri_3
];

/*
 * Definition of global variables.
 */

var pipelines = [null, null, null];
var plumbers = [null, null, null];

var idPipeline = 0;
var idCounter = 0;

var master = null;
var viewers = {};

var clients = [null, null, null];

function getPipeLineId() {
	return (idPipeline++ % (pipelines.length - 1)) + 1;
}

function nextUniqueId() {
	idCounter++;
	return idCounter.toString();
}

function connect(p1, p2, callback) {
	p2.getAddress(function (error, address) {
		if(error) return onError(error);

		console.log("Got address: " + address);

		p2.getPort (function (error, port) {
			if(error) return onError(error);

			console.log("Got port: " + port);

			p1.link (address, port, function(error, success) {
				callback (error, success);
			});
		});
	});
}

/*
 * Server startup
 */

var asUrl = url.parse(argv.as_uri);
var port = asUrl.port;
var server = app.listen(port, function() {
	console.log('Kurento Tutorial started');
	console.log('Open ' + url.format(asUrl) + ' with a WebRTC capable browser');
});

var WebSocketServer = ws.Server, wss = new WebSocketServer({
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
function getKurentoClientById(id, callback) {
	if (clients[id] !== null) {
		return callback(null, clients[id]);
	}

	kurento(uries[id], function(error, kurentoClient) {
		if (error) {
			console.log("Coult not find media server at address " + uries[id]);
			return callback("Could not find media server at address" + uries[id]
					+ ". Exiting with error " + error);
		}

		clients[id] = kurentoClient;

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

	if (pipelines[0] !== null) {
		stop(id);
	}

	getKurentoClientById(0, function(error, kurentoClient) {
		if (error) {
			stop(id);
			return callback(error);
		}

		if (master === null) {
			return callback('Request was cancelled by the user. You will not be sending any longer');
		}

		kurentoClient.create('MediaPipeline', function(error, pipeline) {
			if (error) {
				return callback(error);
			}

			if (master === null) {
				return callback('Request was cancelled by the user. You will not be sending any longer');
			}

			pipelines[0] = pipeline;
			pipelines[0].create('WebRtcEndpoint', function(error, webRtcEndpoint) {
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

					callback( null, sdpAnswer);
				});
			});
		});
	});
}

function connectMasterToPlumber(error, success, pipelineId, callback)
{
	var mainPlumbers = plumbers[0];

	if (error === null && !success) {
		error = "Could not connect plumbers"
	}

	if (error && !mainPlumbers[pipelineId].connected) {
		/* If we couldn't connect remote pipeline we should check if it is */
		/* already connected, if so this error happened because a concurrent */
		/* connection happened and the first try was successful */
		mainPlumbers[pipelineId].plumber.release();
		mainPlumbers[pipelineId].plumber = null;
		return callback (error);
	}

	mainPlumbers[pipelineId].connected = true;

	master.webRtcEndpoint.connect (mainPlumbers[pipelineId].plumber, function(error) {
		if (error) {
			mainPlumbers[pipelineId].plumber.release();
			mainPlumbers[pipelineId].plumber = null;
			return callback (error);
		}

		callback(null);
	});
}

function connectToMainPipeline (pipelineId, callback) {
	var mainPlumbers = plumbers[0];

	if (mainPlumbers === null) {
		mainPlumbers = {};
		plumbers[0] = mainPlumbers;
	}

	if (!mainPlumbers[pipelineId]) {
		mainPlumbers[pipelineId] = {
			plumber : null,
			connected : false
		};
	}

	if (mainPlumbers[pipelineId].plumber != null) {
		if (mainPlumbers[pipelineId].connected) {
			return callback(null);
		}

		return connect (mainPlumbers[pipelineId].plumber, plumbers[pipelineId], function(error, success) {
			connectMasterToPlumber (error, success, pipelineId, callback);
		});
	}

	pipelines[0].create("PlumberEndpoint", function(error, plumberEndPoint) {
		if (error) {
			return callback(error);
		}

		mainPlumbers[pipelineId].plumber = plumberEndPoint;

		connect (plumberEndPoint, plumbers[pipelineId], function(error, success) {
			connectMasterToPlumber (error, success, pipelineId, callback);
		});
	});
}

function createPeerWebRTC (id, sdp, ws, pipelineId, callback) {
	pipelines[pipelineId].create('WebRtcEndpoint', function(error, webRtcEndpoint) {
		if (error) {
			return callback(error);
		}

		viewers[id] = {
			id : id,
			ws : ws,
			webRtcEndpoint : webRtcEndpoint
		}

		if (master === null) {
			stop(id);
			return callback("No active sender now. Become sender or . Try again later ...");
		}

		webRtcEndpoint.processOffer(sdp, function(error, sdpAnswer) {
			if (error) {
				stop(id);
				return callback(error);
			}

			if (master === null) {
				stop(id);
				return callback("No active sender now. Become sender or . Try again later ...");
			}

			plumbers[pipelineId].connect(webRtcEndpoint, function(error) {
				if (error) {
					stop(id);
					return callback(error);
				}

				connectToMainPipeline(pipelineId, function (error) {
					if (error) {
						stop(id);
						return callback(error);
					}

					if (master === null) {
						stop(id);
						return callback("No active sender now. Become sender or . Try again later ...");
					}

					return callback(null, sdpAnswer);
				});
			});
		});
	});
}

function createPlumber (id, sdp, ws, pipelineId, callback) {

	if (plumbers[pipelineId] !== null) {
		return createPeerWebRTC (id, sdp, ws, pipelineId, callback);
	}

	pipelines[pipelineId].create("PlumberEndpoint", function(error, plumberEndPoint) {
		if (error) {
			callback(error);
		}

		console.log ("Created plumber " + pipelineId);

		plumbers[pipelineId] = plumberEndPoint;
		createPeerWebRTC (id, sdp, ws, pipelineId, callback);
	});
}

function getPeerPipeline(callback) {
	var pipelineId;

	pipelineId = getPipeLineId();

	if (pipelines[pipelineId] !== null) {
		return callback (null, pipelineId);
	}

	getKurentoClientById(pipelineId, function(error, kurentoClient) {
		if (error) {
			return callback(error);
		}

		kurentoClient.create('MediaPipeline', function(error, pipeline) {
			if (error) {
				return callback (error, null);
			}

			console.log ("Create pipeline " + pipelineId);
			pipelines[pipelineId] = pipeline;

			callback (null, pipelineId);
		});
	});
}

function startViewer(id, sdp, ws, callback) {
	if (master === null || master.webRtcEndpoint === null) {
		return callback("No active sender now. Become sender or . Try again later ...");
	}

	if (viewers[id]) {
		return callback("You are already viewing in this session. Use a different browser to add additional viewers.")
	}

	getPeerPipeline (function (error, pipelineId) {
		if (error) {
			return callback("Could not get remote pipeline for this peer.");
		}

		createPlumber (id, sdp, ws, pipelineId, callback);
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

function release() {
	var i;

	for (i = 0; i < pipelines.length; i++) {
		if (!pipelines[i])
			continue;

		pipelines[i].release();
		pipelines[i] = null;
	}

	for ( var ix in plumbers[0]) {
		plumbers[0][ix].plumber.release();
		plumbers[0][ix].plumber = null;
		plumbers[0][ix].connected = false;
		delete plumbers[0][ix];
	}

	for (i = 1; i < plumbers.length; i++) {
		if (plumbers[i] === null) {
			continue;
		}

		plumbers[i].release();
		plumbers[i] = null;
	}

	viewers = {};
	master = null;
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
		release ();
	} else if (viewers[id]) {
		var viewer = viewers[id];
		if (viewer.webRtcEndpoint)
			viewer.webRtcEndpoint.release();
		delete viewers[id];
	}
}

app.use(express.static(path.join(__dirname, 'static')));
