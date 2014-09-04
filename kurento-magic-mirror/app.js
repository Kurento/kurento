var KwsMedia = require('kws-media-api'); //TODO: use kurento-client-js
var express = require('express');
var app = express();
var path = require('path');
var wsm = require('ws');

app.set('port', process.env.PORT || 3000);

/*
* Defintion of constants
**/

const ws_uri = "ws://192.168.56.101:8888/kurento"; //TODO: set to localhost.

/*
 * Definition of global variables.
 * */

var pipeline = null;


/*
* Server startup
**/

var port = app.get('port');
var server = app.listen(port, function(){
    console.log('Express server started ');
    console.log('Connect to http://localhost:' + port + '/');
});

var WebSocketServer = wsm.Server
, wss = new WebSocketServer({server: server, path: '/magicmirror'});

wss.on('connection', function(ws) {
    
    ws.on('message', function(_message) {
	var message = JSON.parse(_message);
	console.log('received: ', message);
	
	switch(message.id){
	    case 'start':
		start(message.sdpOffer, function(error, sdpAnswer){
		    if(error){
			ws.send(JSON.stringify({id:'error', message : error}));
		    }
		    ws.send(JSON.stringify({id:'startResponse', sdpAnswer:sdpAnswer}));
		});
		break;
	    
	    case 'stop':
		stop();
		break;
	    
	    default:
		ws.send(JSON.stringify({id:'error', message : 'Invalid message ' + message}));
		break;
	}
	
    });
});

/*
 * Definition of functions
 * */

function getPipeline(callback){
	if(pipeline !== null){
		return callback(null, pipeline);
	}
	
	KwsMedia(ws_uri, function(error, kurentoClient){
		if(error){
			return callback(error);
		}
		
		kurentoClient.create('MediaPipeline', function(error, _pipeline){
			if(error){
				return callback(error);
			}
			
			pipeline = _pipeline;
			return callback(null, pipeline);
		});
	});
}


function start(sdpOffer, callback){
    
    getPipeline(function(error, pipeline){
        if(error){
            return callback(error);
        }
        
        pipeline.create('WebRtcEndpoint', function(error, webRtcEndpoint){
            if(error){
                return callback(error);
            }
            
            webRtcEndpoint.processOffer(sdpOffer, function(error, sdpAnswer){
                if(error){
                    webRtcEndpoint.release();
                    return callback(error);
                }
                
                webRtcEndpoint.connect(webRtcEndpoint, function(error){
		    callback(null, sdpAnswer);
                });
            });            
	});        
    });
}

function stop() {
    if(pipeline !== null){
	pipeline.release();
    }
}


app.use(express.static(path.join(__dirname, 'static')));
