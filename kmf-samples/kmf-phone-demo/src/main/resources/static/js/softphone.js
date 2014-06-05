function Softphone(wsUrl, videoInput, videoOutput)
{
	var client = new JsonRpcClient(wsUrl, onRequest);

	var localPeerConnection;


	// Process request messages

	function onRequest(request) {
		switch(request.method) {
		case 'incommingCall':
			onIncommingCall(request);
			break;

		case 'startCommunication':
			onStartCommunication(request);
			break;

		default:
			console.error('Unrecognized request', request);
		}
	};
	
	var webRtcPeer;
	function onIncommingCall(request) {
		webRtcPeer = kwsUtils.WebRtcPeer.startSendRecv(videoInput, videoOutput, function(offerSdp, wp) {
			var response = {
					callResponse: 'Accept',
					sdpOffer: offerSdp
			};

			request.reply(null, response);
		});

	};

	function onStartCommunication(request) {
		var sdpAnswer = request.params.sdpAnswer;
		webRtcPeer.processSdpAnswer(sdpAnswer);
		request.reply(null, {});
	};

	// Public API

	this.register = function(name) {
		var params = {name: name};

		client.sendRequest('register', params, function(error) {
			if(error) return onerror(error);
			console.log('registered');
		});
	};

	this.call = function(peer) {
		kwsUtils.WebRtcPeer.startSendRecv(videoInput, videoOutput, function(offerSdp, wp) {
			console.log('Invoking SDP offer callback function');
			var params = {callTo: peer, sdpOffer: offerSdp};
			client.sendRequest('call', params, function(error, result) {
				if(error) return onerror(error);
				wp.processSdpAnswer(result.sdpAnswer);
			});
		});
	};
}
