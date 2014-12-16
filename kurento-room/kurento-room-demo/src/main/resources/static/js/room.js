var kurento;
var room;
var selectedParticipantDiv;

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
			userName : name
		// FIXME: user name should go in stream attributes
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

			var updateMainVideo = function(smallVideoElem, stream) {
				$(selectedParticipantDiv).removeClass("active-video");
				var mainVideo = document.getElementById("main-video");
				var oldVideo = mainVideo.firstChild;
				stream.playOnlyVideo("main-video");
				smallVideoElem.className += " active-video";
				selectedParticipantDiv = smallVideoElem;
				if (oldVideo !== null) {
					mainVideo.removeChild(oldVideo);
				}
			}

			var playVideo = function(elementId, stream) {

				var div = document.createElement('div');
				div.setAttribute("id", elementId);
				div.className = "video";
				document.getElementById("participants").appendChild(div);

				$(div).click(function() {
					updateMainVideo(div, stream);
				});

				stream.play(elementId);

				updateMainVideo(div, stream);
			};

			var updateVideoStyle = function() {
				var numParticipants = Object.keys(room.getStreams()).length;
				if (numParticipants > 5) {
					$('.video').css({
						"width" : (95 / numParticipants) + "%"
					});
				} else {
					$('.video').css({
						"width" : "19%"
					});
				}
			}

			room.addEventListener("room-connected", function(roomEvent) {

				document.getElementById('room-name').innerText = room.name;
				document.getElementById('join').style.display = 'none';
				document.getElementById('room').style.display = 'block';

				room.publish(localStream);
				subscribeToStreams(roomEvent.streams);
			});

			room.addEventListener("stream-subscribed", function(streamEvent) {

				var stream = streamEvent.stream;

				playVideo("video-" + stream.getID(), stream);

				updateVideoStyle();
			});

			room.addEventListener("stream-added", function(streamEvent) {
				var streams = [];
				streams.push(streamEvent.stream);
				subscribeToStreams(streams);
			});

			room.addEventListener("stream-removed", function(streamEvent) {
				var element = document.getElementById("video-"
						+ streamEvent.stream.getID());
				if (element !== undefined) {
					element.parentNode.removeChild(element);
				}
				updateVideoStyle();
			});

			room.connect();

			playVideo("video-" + name, localStream);
		});

		localStream.init();

	});
}

function leaveRoom() {

	document.getElementById('join').style.display = 'block';
	document.getElementById('room').style.display = 'none';

	var streams = room.getStreams();
	for ( var index in streams) {
		var stream = streams[index];
		var element = document.getElementById("video-" + stream.getID());
		if (element) {
			element.parentNode.removeChild(element);
		}
	}

	room.leave();
}

window.onbeforeunload = function() {
	kurento.close();
};

function toggleFullScreen() {

	var videoElement = document.getElementById("room");
	
	try {
		if (!document.mozFullScreen && !document.webkitFullScreen) {
			if (videoElement.mozRequestFullScreen) {
				videoElement.mozRequestFullScreen();
			} else {
				videoElement
						.webkitRequestFullScreen(Element.ALLOW_KEYBOARD_INPUT);
			}
		} else {
			if (document.mozCancelFullScreen) {
				document.mozCancelFullScreen();
			} else {
				document.webkitCancelFullScreen();
			}
		}
	} catch (e) {
		console.error(e);
	}
}
