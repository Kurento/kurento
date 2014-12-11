var kurentoRoom;

window.onload = function() {
	console = new Console('console', console);
}

function register() {

	kurentoRoom = KurentoRoomClient(
		{
			wsUri : 'ws://' + location.host + '/room',
			participantsId : 'participants'
		},
		function(error, kurentoRoom) {

			if (error) return console.log(error);

			var name = document.getElementById('name').value;
			var room = document.getElementById('roomName').value;

			document.getElementById('room-header').innerText = 'ROOM ' + room;
			document.getElementById('join').style.display = 'none';
			document.getElementById('room').style.display = 'block';

			kurentoRoom.joinRoom(name,room);

	});
}

function leaveRoom(){
	kurentoRoom.leaveRoom();
	
}

window.onbeforeunload = function() {
	kurentoRoom.close();
};
