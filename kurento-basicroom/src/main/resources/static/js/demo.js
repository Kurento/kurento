var kurento;
var room;

window.onload = function() {
	console = new Console('console', console);
}

var kurento;
var room;

window.onload = function() {
	console = new Console('console', console);
}

function register() {

	var name = document.getElementById('name').value;
	var roomName = document.getElementById('roomName').value;

	var wsUri = 'ws://' + location.host + '/room';

	kurento = KurentoRoom(wsUri, function(error, kurento) {

		if (error)
			return console.log(error);

		var localStream = kurento.Stream({
			audio : true,
			video : true,
			data : true,
			name : name
		});

		// L: La room se crea SIEMPRE en el servidor y el cliente se conecta
		// var room = KurentoRoom.Room({
		// token : "af54/=gopknosdvmgiufhgadf=="
		// });

		// K: Crea la room desde el cliente
		room = kurento.Room({
			name : roomName,
			userName : name // FIXME: user name should go in stream attributes
		});

		localStream.addEventListener("access-accepted", function() {

			var subscribeToStreams = function(streams) {
				for (var index in streams) {
					var stream = streams[index];
					if (localStream.getID() !== stream.getID()) {
						room.subscribe(stream);
					}
				}
			};

			room.addEventListener("room-connected", function(roomEvent) {

				document.getElementById('room-header').innerText = 'ROOM \"' + room.name+'\"';
				document.getElementById('join').style.display = 'none';
				document.getElementById('room').style.display = 'block';

				room.publish(localStream);
				subscribeToStreams(roomEvent.streams);
			});

			room.addEventListener("stream-subscribed", function(streamEvent) {

				var stream = streamEvent.stream;

				var elementId = "video-" + stream.getID();
				var div = document.createElement('div');
				div.setAttribute("id", elementId);
				document.getElementById("participants").appendChild(div);

				stream.play(elementId);
			});

			room.addEventListener("stream-added", function(streamEvent) {
				var streams = [];
				streams.push(streamEvent.stream);
				subscribeToStreams(streams);
			});

			room.addEventListener("stream-removed", function(streamEvent) {
				var element = document.getElementById("video-"+streamEvent.stream.getID());
				if (element !== undefined) {
					element.parentNode.removeChild(element);
				}
			});

			room.connect();

			var element = document.getElementById("myVideo");
			var video = document.createElement('div');
			video.id = "video-"+name;
			element.appendChild(video);

			localStream.play(video.id);
		});

		localStream.init();

	});
}

function leaveRoom(){

	document.getElementById('join').style.display = 'block';
	document.getElementById('room').style.display = 'none';

	var streams = room.getStreams();
	for (var index in streams) {
		var stream = streams[index];
		var element = document.getElementById("video-"+stream.getID());
		if (element) {
			element.parentNode.removeChild(element);
		}
	}

	room.leave();
}

window.onbeforeunload = function() {
	kurento.close();
};