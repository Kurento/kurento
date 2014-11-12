#!/usr/bin/env node
/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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
var url = require('url');
var kurento = require('kurento-client');

var argv = minimist(process.argv.slice(2),
{
  default:
  {
    as_uri: "http://localhost:8080/",
    ws_uri: "ws://localhost:8888/kurento"
  }
});

var app = express();
var bodyParser = require('body-parser');
app.use(bodyParser.text({type : 'application/sdp'}));

/*
 * Definition of global variables.
 */

var pipeline = null;

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
 * Definition of functions
 */
function sendError(res, code, error) {
	console.log("Error " + error);
	res.type('text/plain');
	res.status(code);
	res.send(error);
}

/*
 * Code for recovering a media pipeline.
 */
function getPipeline(callback) {
	if (pipeline !== null) {
		return callback(null, pipeline);
	}

	kurento(argv.ws_uri, function(error, kurentoClient) {
		if (error) {
			return callback(error);
		}

		kurentoClient.create('MediaPipeline', function(error, _pipeline) {
			if (error) {
				return callback(error);
			}

			pipeline = _pipeline;
			return callback(null, pipeline);
		});
	});
}

/*
 * Code for processing REST request from client.
 */
app.post('/helloworld', function(req, res) {
	var sdpOffer = req.body;

	console.log("Request received from client with sdpOffer =", sdpOffer);

	console.log("Obtaining MediaPipeline");
	getPipeline(function(error, pipeline) {
		if (error) {
			return sendError(res, 500, error);
		}
		
		
		console.log("Creating WebRtcEndpoint");
		pipeline.create('WebRtcEndpoint', function(error, webRtcEndpoint) {
			if (error) {
				return sendError(res, 500, error);
			}

			console.log("Processing sdpOffer at server and generating sdpAnswer");
			webRtcEndpoint.processOffer(sdpOffer, function(error, sdpAnswer) {
				if (error) {
					webRtcEndpoint.release();
					return sendError(res, 500, error);
				}

				console.log("Connecting loopback");
				webRtcEndpoint.connect(webRtcEndpoint, function(error) {
					if(error){
						webRtcEndpoint.release();
						return sendError(res, 500, error);
					}
					console.log("Sending sdpAnswer to client");
					console.log(sdpAnswer);
					
					res.type('application/sdp');
					res.send(sdpAnswer);
				});
			});
		});
	});
});

app.use(express.static(path.join(__dirname, 'static')));
