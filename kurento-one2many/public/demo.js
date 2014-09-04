

var socket = io();

const ERROR = -1;
const UNKNOWN = 0;
const I_CAN_BE_SENDER = 1;
const I_AM_SENDER = 2;
const I_CAN_BE_RECEIVER = 3;
const I_AM_RECEIVER = 4;


var state = null;
var webRtcPeer = null;

function onError(error, state){
	if(error){
		window.alert(error);
	}
	if(state){
		setState(error, state);
	}
}

function setState (nextState){
	
	state = nextState;
	
	switch(state){
		case ERROR:
			//reload
			break;
		case UNKNOWN:
			//disable all buttons
			break;
		case I_CAN_BE_SENDER:
			//only addSenderButton enabled
			break;
		case I_AM_SENDER:
			//only removeSenderButton enabled
			break;
		case I_CAN_BE_RECEIVER:
			//only addReceiverButton enabled
			break;
		case I_AM_RECEIVER:
			//only removeReceiverButton enabled
			break;
		default:
		
	}	
	
	console.log("State changed to " + state);	
}

window.addEventListener('load', function(){
	
	console.log("Starting program execution...");
	
	setState(UNKNOWN);

	socket.on('setClientState', function(error, nextState){
		if(error){
			return onError(error, nextState);
		}
		setState(nextState);
	});
	
	var addSenderButton = document.getElementById('addSenderButton');
	addSenderButton.addEventListener('click', startSending);
	
	var addReceiverButton = document.getElementById('addReceiverButton');
	addReceiverButton.addEventListener('click', startReceiving);
	
	var removeSenderButton = document.getElementById('removeSenderButton');
	removeSenderButton.addEventListener('click', cancelSending);
	
	var removeReceiverButton = document.getElementById('removeReceiverButton');
	removeReceiverButton.addEventListener('click', cancelReceiving);
	
});

function cancelSending(){
	if(state !== I_AM_SENDER){
		return onError("Cannot cancel sending while not being sender");
	}
	setState(I_CAN_BE_SENDER);
	socket.emit("removeSender");
	if(webRtcPeer !== null){
		webRtcPeer.dispose();
	}
}

function cancelReceiving(){
	if(state !== I_AM_RECEIVER){
		return onError("Cannot cancel receiving while not being receiver");
	}
	setState(I_CAN_BE_RECEIVER);
	socket.emit("removeReceiver");
	if(webRtcPeer !== null){
		webRtcPeer.dispose();
	}
}

function startSending(){
	if(state !== I_CAN_BE_SENDER){
		return onError("Cannot start sending now");
	}
	console.log("Trying to become sender ...");
	setState(I_AM_SENDER)
	var localVideo = document.getElementById('localVideo');
	var remoteVideo = document.getElementById('remoteVideo');
	
	console.log("Creating webRtcPeer for SendRecv ...");
	webRtcPeer = kwsUtils.WebRtcPeer.startSendRecv(localVideo, remoteVideo, onOffer, onError);
	
	function onOffer(offer){
		console.log("Local offer obtained. Looking for sdpAnswer ...");
		socket.emit('addSender', offer, function(error, state, sdpAnswer){
			if(error){
				return onError(error, state);
			}
			console.log("Response sdpAnswer received. Starting SendRecv WebRTC flow");
			webRtcPeer.processSdpAnswer(sdpAnswer);
		});
	}	
}

function startReceiving(){
	if(state !== I_CAN_BE_RECEIVER){
		return onError("Cannot start receiving now");
	}
	console.log("Trying to become a receiver");
	setState(I_AM_RECEIVER);
	var remoteVideo = document.getElementById('remoteVideo');

	console.log("Creating webRtcPeer for RecvOnly");
	webRtcPeer = kwsUtils.WebRtcPeer.startRecvOnly(remoteVideo, onOffer, onError);
	
	function onOffer(offer){
		console.log("Local offer obtained. Looking for sdpAnswer ...");
		socket.emit('addReceiver', offer, function(error, state, sdpAnswer){
			if(error){
				return onError(error, state);
			}
			console.log("Response sdpAnswer received. Starting RecvOnly WebRTC flow");
			webRtcPeer.processSdpAnswer(sdpAnswer);
		});
	}
}