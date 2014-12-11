

var room = Woogeen.Room({token: token});

var localStream = Woogeen.Stream({audio: true, video: true});

localStream.addEventListener("access-accepted", function () {

	room.addEventListener("room-connected", function (roomEvent) {
		var streams = roomEvent.streams;
		for (var index in streams) {
			var stream = streams[index];
			room.subscribe(stream);
		}
		room.publish(localStream);
	});

	room.addEventListener("stream-subscribed", function(streamEvent) {
		var stream = streamEvent.stream;
		var div = document.createElement('div');
		div.setAttribute("style", "width: 320px; height: 240px;");
		div.setAttribute("id", "test" + stream.getID());
		document.getElementById('conference').appendChild(div);
		stream.show("test" + stream.getID());
	});

	room.addEventListener("stream-added", function (streamEvent) {
		//...
	});

	room.addEventListener("stream-removed", function (streamEvent) {
		//...
	});

	room.addEventListener('stream-changed', function (streamEvent) {
		showmsg('stream '+streamEvent.stream.getID()+' has changed', 2000, 4000);
	});

	room.addEventListener('client-joined', function (streamEvent) {
		showmsg(streamEvent.user.name+'.'+streamEvent.user.role+'.'+streamEvent.attr.s
				id+' has joined the room', 2000, 4000);
	});

	room.addEventListener('client-left', function (streamEvent) {
		showmsg(streamEvent.user.name+'.'+streamEvent.user.role+'.'+streamEvent.attr.s
				id+' has left the room', 2000, 4000);
	});

	room.connect();

	localStream.show("myVideo");
});

localStream.init();