var rtcStats = {};

function activateRtcStats() {
	var rate = 100; // by default each 100 ms
	if (arguments.length) {
		rate = arguments[0];
	}
	setInterval(updateRtcStats, rate);
}

function updateRtcStats() {
	var remoteStream = webRtcPeer.peerConnection.getRemoteStreams()[0];
	var videoTrack = remoteStream.getVideoTracks()[0];
	var audioTrack = remoteStream.getAudioTracks()[0];

	updateStats(videoTrack, "video_");
	updateStats(audioTrack, "audio_");
}

function updateStats(track, type) {
	webRtcPeer.peerConnection.getStats(function(stats) {
		var result = stats.result()[2];
		result.names().forEach(function(name) {
			rtcStats[type + name] = result.stat(name);
			// console.info(name + "=" + result.stat(name));
		});
	}, track);
}
