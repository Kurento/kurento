// Room --------------------------------

function Room(kurento, options) {

	var that = this;

	that.name = options.name;

	var ee = new EventEmitter();
	var streams = {};

	this.addEventListener = function(eventName, listener) {
		ee.addListener(eventName, listener);
	}

	this.emitEvent = function(eventName, eventsArray) {
		ee.emitEvent(eventName, eventsArray);
	}

	this.connect = function() {

		kurento.sendMessage({
			id : 'joinRoom',
			name : options.userName, // FIXME: User name should go in stream
			// attributes
			room : options.name,
		});
	}

	// Request response
	this.onExistingParticipants = function(msg) {

		var roomEvent = {
			streams : []
		}

		var length = msg.data.length;
		for (var i = 0; i < length; i++) {
			var userName = msg.data[i];
			var stream = new Stream(kurento, false, {
				name : userName
			});
			streams[userName] = stream;
			roomEvent.streams.push(stream);
		}

		ee.emitEvent('room-connected', [ roomEvent ]);
	}

	this.subscribe = function(stream) {
		stream.room = that;
		stream.subscribe();
	}

	this.publish = function(localStream) {
		localStream.room = that;
		streams[localStream.getID()] = localStream;
		localStream.publish();
	}

	this.onNewParticipant = function(msg) {
		var stream = new Stream(kurento, false, {
			name : msg.name
		});
		streams[msg.name] = stream;
		ee.emitEvent('stream-added', [ {
			stream : stream
		} ]);
	}

	this.onParticipantLeft = function(msg) {
		var stream = streams[msg.name];
		if (stream !== undefined) {
			delete streams[msg.name];
			ee.emitEvent('stream-removed', [ {
				stream : stream
			} ]);
			stream.dispose();
		}
	}

	this.receiveVideoResponse = function(msg) {
		var stream = streams[msg.name];
		if (stream !== undefined) {
			stream.processSdpAnswer(msg.sdpAnswer);
		} else {
			console.warn("Receiving video response from an unexisting user: "
					+ msg.name);
		}
	}

	this.leave = function() {

		kurento.sendMessage({
			id : 'leaveRoom'
		});

		for (var key in streams) {
			streams[key].dispose();
		}
	}

	this.getStreams = function(){
		return streams;
	}
}

// Stream --------------------------------

function Stream(kurento, local, options) {

	var that = this;

	this.room = undefined;

	var ee = new EventEmitter();
	var sdpOffer;
	var wrStream;
	var wp;
	var id = options.name;

	this.addEventListener = function(eventName, listener) {
		ee.addListener(eventName, listener);
	}

	this.play = function(elementId) {

		var container = document.createElement('div');
		container.className = "participant";
		container.id = id;

		that.element = container;

		var span = document.createElement('span');
		var video = document.createElement('video');

		container.appendChild(video);
		container.appendChild(span);

		document.getElementById(elementId).appendChild(container);

		span.appendChild(document.createTextNode(id));

		video.id = 'native-video-' + id;
		video.autoplay = true;
		video.controls = false;
		video.src = URL.createObjectURL(wrStream);
		video.muter = true;
	}

	this.getID = function() {
		return id;
	}

	this.init = function() {

		var constraints = {
			audio : true,
			video : {
				mandatory : {
					maxWidth : 320,
					maxFrameRate : 15,
					minFrameRate : 15
				}
			}
		};

		getUserMedia(constraints, function(userStream) {
			wrStream = userStream;
			ee.emitEvent('access-accepted', null);
		}, function(error) {
			console.error(error);
		});
	}

	function initWebRtcPeer(sdpOfferCallback) {

		var startVideoCallback = function(sdpOfferParam) {
			sdpOffer = sdpOfferParam;
			kurento.sendMessage({
				id : "receiveVideoFrom",
				sender : id,
				sdpOffer : sdpOffer
			});
		}

		var onerror = function(error) {
			console.error(error);
		}

		var mode = local ? "send" : "recv";

		wp = new kurentoUtils.WebRtcPeer(mode, null, null, startVideoCallback,
				onerror, null, null);

		wp.stream = wrStream;
		wp.start();

		console.log(name + " waiting for SDP offer");
	}

	this.publish = function() {

		// FIXME: Throw error when stream is not local

		initWebRtcPeer();

		// FIXME: Now we have coupled connecting to a room and adding a
		// stream to this room. But in the new API, there are two steps.
		// This is the second step. For now, it do nothing.

	}

	this.subscribe = function() {

		// FIXME: In the current implementation all participants are subscribed
		// automatically to all other participants. We use this method only to
		// negotiate SDP

		// Refactor this to avoid code duplication
		initWebRtcPeer();
	}

	this.processSdpAnswer = function(sdpAnswer) {

		var answer = new RTCSessionDescription({
			type : 'answer',
			sdp : sdpAnswer,
		});

		console.log('SDP answer received, setting remote description');

		wp.pc.setRemoteDescription(answer, function() {
			if (!local) {
				// FIXME: This avoid to subscribe to your own stream remotely.
				// Fix this
				wrStream = wp.pc.getRemoteStreams()[0];
				that.room.emitEvent('stream-subscribed', [ {
					stream : that
				} ]);
			}
		}, function(error) {
			console.error(error)
		});
	}

	this.dispose = function() {
		console.log("disposed");
		if (that.element !== undefined) {
			that.element.parentNode.removeChild(that.element);
		}

		if (wp.pc && wp.pc.signalingState != 'closed')
			wp.pc.close();

		if (wrStream) {
			wrStream.getAudioTracks().forEach(function(track) {
				track.stop && track.stop()
			})
			wrStream.getVideoTracks().forEach(function(track) {
				track.stop && track.stop()
			})
		}
	}
}

// KurentoRoom --------------------------------

function KurentoRoom(wsUri, callback) {

	if (!(this instanceof KurentoRoom))
		return new KurentoRoom(wsUri, callback);

	// Enable and disable iceServers from code
	kurentoUtils.WebRtcPeer.prototype.server.iceServers = [];

	var that = this;

	var userName;
	var ws = new WebSocket(wsUri);

	ws.onopen = function() {
		callback(null, that);
	}

	ws.onerror = function(evt) {
		callback(evt.data);
	}

	ws.onclose = function() {
		console.log("Connection Closed");
	}

	ws.onmessage = function(message) {

		var parsedMessage = JSON.parse(message.data);
		console.info('Received message: ' + message.data);

		switch (parsedMessage.id) {
		case 'existingParticipants':
			onExistingParticipants(parsedMessage);
			break;
		case 'newParticipantArrived':
			onNewParticipant(parsedMessage);
			break;
		case 'participantLeft':
			onParticipantLeft(parsedMessage);
			break;
		case 'receiveVideoAnswer':
			receiveVideoResponse(parsedMessage);
			break;
		default:
			console.error('Unrecognized message', parsedMessage);
		}
	}

	function onNewParticipant(msg) {
		if (room !== undefined) {
			room.onNewParticipant(msg);
		}
	}

	function onParticipantLeft(msg) {
		if (room !== undefined) {
			room.onParticipantLeft(msg);
		}
	}

	function onExistingParticipants(msg) {
		if (room !== undefined) {
			room.onExistingParticipants(msg);
		}
	}

	function receiveVideoResponse(result) {
		if (room !== undefined) {
			room.receiveVideoResponse(result);
		}
	}

	this.sendMessage = function(message) {
		var jsonMessage = JSON.stringify(message);
		console.log('Sending message: ' + jsonMessage);
		ws.send(jsonMessage);
		console.log('Sent message: ' + jsonMessage);
	}

	this.close = function() {
		if (room !== undefined) {
			room.leave();
		}
		ws.close();
	}

	this.Stream = function(options) {
		return new Stream(that, true, options);
	}

	this.Room = function(options) {
		// FIXME Support more than one room
		room = new Room(that, options);
		// FIXME Include name in stream, not in room
		usarName = options.userName;
		return room;
	}
}