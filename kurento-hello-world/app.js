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

var kurento = require('kurento-client');
var express = require('express');
var app = express();
var bodyParser = require('body-parser');
app.use(bodyParser.text({type : 'application/sdp'}));
var path = require('path');
app.set('port', process.env.PORT || 8080);

/*
 * Definition of constants
 */


//YOU MUST SET THIS TO THE HOST:PORT WHERE YOUR MEDIA SERVER IS LOCATED
const ws_uri = "ws://localhost:8888/kurento";

/*
 * Definition of global variables.
 */

var pipeline = null;

/*
 * Server startup
 */

var port = app.get('port');
var server = app.listen(port, function() {
	console.log('Express server started ');
	console.log('Connect to http://<host_name>:' + port + '/');
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
 * */
function getPipeline(callback) {
	if (pipeline !== null) {
		return callback(null, pipeline);
	}

	kurento(ws_uri, function(error, kurentoClient) {
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
 * */
app.post('/helloworld', function(req, res) {
	var sdpOffer = req.body;

	console.log("Request received from client with sdpOffer = ");
	console.log(sdpOffer);
	
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
