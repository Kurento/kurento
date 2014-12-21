function KurentoBasicRoom(wsUri, callback) {

	if (!(this instanceof KurentoBasicRoom))
		return new KurentoBasicRoom(wsUri, callback);

	var that = this;

	KurentoRoom(wsUri, function(error, kurento) {

		if (error)
			return callback(error, this);

		var ee = new EventEmitter();
		var room;


		that.addEventListener = function(eventName, listener){
			ee.addListener(eventName, listener);
		}

		that.leaveRoom = function(options){
			room.leave();
		}

		that.joinRoom = function(options){

			var localStream = kurento.Stream({
				audio : true,
				video : true,
				data : true,
				name : options.userName
			});

			room = kurento.Room({
				name : options.roomName,
				userName : options.userName
			});

			localStream.addEventListener("access-accepted", function() {

				var subscribeToStreams = function(streams) {
					for ( var index in streams) {
						var stream = streams[index];
						if (localStream.getID() !== stream.getID()) {
							room.subscribe(stream);
						}
					}
				};

				room.addEventListener("room-connected", function(roomEvent) {
					room.publish(localStream);
					subscribeToStreams(roomEvent.streams);
					roomEvent.localStream = localStream;
					ee.emitEvent("room-connected", [roomEvent]);
				});

				room.addEventListener("stream-subscribed", function(streamEvent) {
					ee.emitEvent("stream-added", [streamEvent]);
				});

				room.addEventListener("stream-added", function(streamEvent) {
					var streams = [];
					streams.push(streamEvent.stream);
					subscribeToStreams(streams);
				});

				room.addEventListener("stream-removed", function(streamEvent) {
					ee.emitEvent("stream-removed", [streamEvent]);
				});

				room.connect();
			});

			localStream.init();
		}

		callback(null, that);
	});
}
