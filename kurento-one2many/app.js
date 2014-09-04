var KwsMedia = require('kws-media-api');
var express = require('express');
var app = express();
var path = require('path');

var server = app.listen(3456, function(){
	  console.log('Express server started ');
          console.log('Connect to http://localhost:3456/demo.html')
	});

var io = require('socket.io').listen(server);

/*
* Defintion of constants
**/

const ERROR = -1;
const UNKNOWN = 0;
const I_CAN_BE_SENDER = 1;
const I_AM_SENDER = 2;
const I_CAN_BE_RECEIVER = 3;
const I_AM_RECEIVER = 4;

const ws_uri = "ws://192.168.56.101:8888/kurento";

/*
 * Definition of global variables.
 * */

var sender = null;
var receivers = {};
var pipeline = null;


/*
 * Definition of functions
 * */

function getState4Client(){
          if(sender === null){
                    return I_CAN_BE_SENDER;
          } else {
                    return I_CAN_BE_RECEIVER;
          }
}

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

function addSender(id, sdp, callback){
	if(sender !== null){
		return callback("There is already an active sender. Try again later ...", getState4Client());
	}
	
	sender = {
		id : id,
		webRtcEndpoint : null
	};
	
	getPipeline(function(error, pipeline){
		if(error){
			sender = null;
			return callback(error, getState4Client());
		}
		
		pipeline.create('WebRtcEndpoint', function(error, webRtcEndpoint){
			if(error){
				sender = null;
				return callback(error, getState4Client());
			}
			
			sender.webRtcEndpoint = webRtcEndpoint;
			
			webRtcEndpoint.processOffer(sdp, function(error, sdpAnswer){
				if(error){
					webRtcEndpoint.release();
					sender = null;
					return callback(error, getState4Client());
				}
				
				webRtcEndpoint.connect(webRtcEndpoint, function (error){
					if(error){
						webRtcEndpoint.release();
						sender = null;
						return callback(error, getState4Client());
					}
					
					callback(null, getState4Client(), sdpAnswer);
				});
			});
		});
	});
}

function addReceiver(id, sdp, callback){
	if(sender === null || sender.webRtcEndpoint === null){
		return callback("Currently there are not active senders. Try again later ...", getState4Client());
	}
	
	pipeline.create('WebRtcEndpoint', function(error, webRtcEndpoint){
		if(error){
			return callback(error, getState4Client());
		}
		
		webRtcEndpoint.processOffer(sdp, function(error, sdpAnswer){
			if(error){
				webRtcEndpoint.release();
				return callback(error, getState4Client());
			}

			sender.webRtcEndpoint.connect(webRtcEndpoint, function(error){
				if(error){
					webRtcEndpoint.release();
					return callback(error, getState4Client());
				}
				
				var receiver = {
						id : id,
						webRtcEndpoint : webRtcEndpoint
				};
				receivers[receiver.id] = receiver;
				
				return callback(null, getState4Client(), sdpAnswer);
			});
		});
	});
}

function removeReceiver(id){
	if(!receivers[id]){
		return;
	}
	var receiver = receivers[id];
	receiver.webRtcEndpoint. release();
	delete receiver[id];
}

function removeSender(){
	if(sender === null){
		return;
	}
	
	for(var ix in receivers){
		removeReceiver(ix);
	}
	
	sender.webRtcEndpoint.release();
	sender = null;
}

io.on('connection', function(socket){
          
	socket.on('addSender', function(sdp, callback){
		addSender(socket.id, sdp, function(error, state, sdpAnswer) {
                    if(error){
                              return callback(error, state);
                    }
                    socket.broadcast.emit('setClientState', null, state);
                    return callback(null, state, sdpAnswer);
          });
	});
	
	socket.on('removeSender', function(){
		removeSender();
                socket.broadcast.emit('setClientState', null, getState4Client());
	});
	
	socket.on('addReceiver', function(sdp, callback){
		addReceiver(socket.id, sdp, callback);             
	});
	
	socket.on('removeReceiver', function(){
		removeReceiver(socket.id);
	});
	
	socket.on('disconnect', function(){
		if(receivers[socket.id]){
			removeReceiver(socket.id);
		} else if (sender !== null && sender.id == socket.id){
			removeSender();
                        socket.broadcast.emit('setClientState', null, getState4Client());
		}
	});
        
        socket.emit('setClientState', null, getState4Client());
});


app.use(express.static(path.join(__dirname, 'public')));

