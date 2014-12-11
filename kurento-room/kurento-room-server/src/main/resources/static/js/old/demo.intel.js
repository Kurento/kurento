var isVideo = 1;

// Please change it to signaling server's address.
var serverAddress = 'ws://' + location.host + '/room';

// Initialize client object
var client = new KurentoRoom.Client({
	iceServers : [ {
		urls : "turn:180.153.223.233:4478?transport=udp",
		credential : "master",
		username : "woogeen"
	}, {
		urls : "turn:180.153.223.233:443?transport=udp",
		credential : "master",
		username : "woogeen"
	}, {
		urls : "turn:180.153.223.233:4478?transport=tcp",
		credential : "master",
		username : "woogeen"
	}, {
		urls : "turn:180.153.223.233:443?transport=tcp",
		credential : "master",
		username : "woogeen"
	}, {
		urls : "stun:180.153.223.233"
	} ]
});

// Token for join a room (obtained from URL in this demo)
var roomToken = JSON.stringify({
	host : serverAddress,
	id : Utils.getQueryStrings()['roomId']
});

// It only initializes a KurentoRoom.Stream object. Using localStream.init() to
// initialize stream.
var localStream = KurentoRoom.Stream({
	audio : true,
	video : true
});

// access-accepted event will be triggered when user accepted to use
// camera/microphone
localStream.addEventListener("access-accepted", function(evt) {
	attachMediaStream($('#local video').get(0), localStream.stream) // Show
																	// local
																	// stream
	client.publish(localStream, roomToken); // Join a chat room.
});

var localScreen = KurentoRoom.Stream({
	screen : true,
	audio : true,
	videoSize : [ 1920, 1080, 1920, 1080 ]
});

// access-accepted event will be triggered when user accepted to use desktop
// screen/microphone
localScreen.addEventListener("access-accepted", function(evt) {
	console.log("localScreen sharing");
	client.publish(localScreen, roomToken); // Publish local desktop screen to
											// remote client
});

$(document).ready(function() {

	$('#login').click(function() {
		// Connect to peer server.
		client.connect({
			host : serverAddress,
			token : $('#uid').val()
		});
		$('#uid').prop('disabled', true);
	});

	$('#connect').click(function() {
		client.joinRoom(roomToken, function() {
			console.log('Joined room');
		}, function() {
			console.log('Failed to join the room.');
		});
	});

	$('#logoff').click(function() {
		// Quit current chat room.
		client.leaveRoom(roomToken);
		$('#uid').prop('disabled', false);
	});

	$('#data-send').click(function() {
		// Send data to peer.
		client.sendData($('#dataSent').val(), roomToken);
	});

	$('#target-screen').click(function() {
		// Initialize local stream.
		localScreen.init();
	});

	$('#target-video-unpublish').click(function() {
		$('#target-video-publish').prop('disabled', false);
		$('#target-video-unpublish').prop('disabled', true);
		client.unpublish(localStream, roomToken);
	});

	$('#target-video-publish').click(function() {
		$('#target-video-unpublish').prop('disabled', false);
		$('#target-video-publish').prop('disabled', true);
		if (localStream.stream) {
			client.publish(localStream, roomToken);
		} else {
			localStream.init();
		}
	});
});

// A remote stream is available.
client.addEventListener('stream-subscribed', function(e) {
	if (e.stream.type === 'video') {
		$('#remote video').show();
		// Show remote video stream.
		attachMediaStream($('#remote video').get(0), e.stream.stream);

	} else if (e.stream.type === 'screen') {
		$('#screen video').show();
		// Show remote screen stream.
		attachMediaStream($('#screen video').get(0), e.stream.stream);
	}
	isVideo++;
});

// Chat stopped
client.addEventListener('chat-stopped', function(e) {
	console.log('chat stopped.');
	$('#dc-create').prop('disabled', true);
	$('#data-send').prop('disabled', true);
	$('#remote video').hide();
});

// Chat started
client.addEventListener('chat-started', function(e) {
	console.log('Video chat is started.');
	$('#dc-create').prop('disabled', false);
	$('#target-screen').prop('disabled', false);
	$('#target-video-publish').prop('disabled', false);
});

// enabled data send with datachannel.
client.addEventListener('data-opened', function(e) {
	$('#data-send').prop('disabled', false);
});

// Received data from datachannel.
client.addEventListener('data-received', function(e) {
	$('#dataReceived').val(e.data);
});