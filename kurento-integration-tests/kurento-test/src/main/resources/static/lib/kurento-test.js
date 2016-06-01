/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

function KurentoTest() {
	// Required for compatibility in color gathering
	window.requestAnimationFrame = window.requestAnimationFrame
			|| window.mozRequestAnimationFrame
			|| window.webkitRequestAnimationFrame
			|| window.msRequestAnimationFrame;

	// Color parameters
	this.x = 0;
	this.y = 0;
	this.colorInfo = {};
	this.maxColorDistance = 60;
	this.colorCheckRate = 10;

	// Latency parameters
	this.latencyVideoTagsId = null;
	this.latencyTime = {};
	this.latency = 0;
	this.firstLatency = true;

	// RTC statistics parameters
	this.rtcStats = {};
	this.rtcStatsRate = 100; // milliseconds
	this.rtcStatsIntervalId = {};

	// Initial time
	this.initTime = new Date();

	// OCR
	this.sync = false;
	this.syncTime = null;
	this.ocrActive = false;
	this.ocrMap = {};
}

KurentoTest.prototype.syncTimeForOcr = function(videoTagId, peerConnectionId) {
	// Sync with clock system
	var timeNow = new Date().getTime();
	this.syncTime = new Date(timeNow + 60000);
	this.syncTime.setSeconds(0);
	this.syncTime.setMilliseconds(0);
	var offsetMillis = this.syncTime.getTime() - timeNow;
	console.info("Time wait for next exact minute: " + offsetMillis + " ms");
	var self = this;

	setTimeout(function() {
		console.info("Sync finished");
		self.sync = true;
		var time = new Date().getTime();
		// Uncomment this line to relative time:
		// var time = new Date().getTime() - self.syncTime.getTime();
		self.getVideoTime(videoTagId, time);
		self.getStats(peerConnectionId, time);

		setInterval(function() {
			var time = new Date().getTime();
			// Uncomment this line to relative time:
			// var time = new Date().getTime() - self.syncTime.getTime();
			self.getVideoTime(videoTagId, time);
			self.getStats(peerConnectionId, time);
		}, 1000);
	}, offsetMillis);
}

KurentoTest.prototype.startOcr = function() {
	console.info("Starting OCR");
	this.ocrActive = true;
}

KurentoTest.prototype.endOcr = function() {
	console.info("Ending OCR");
	this.ocrActive = false;
}

KurentoTest.prototype.getStats = function(peerConnectionId, time) {
	if (this.ocrActive) {
		var peerConnection = eval(peerConnectionId);
		eval("var localStream = peerConnection.getLocalStreams()[0];");
		eval("var remoteStream = peerConnection.getRemoteStreams()[0];");

		var localVideoTrack = localStream ? localStream.getVideoTracks()[0]
				: null;
		var localAudioTrack = localStream ? localStream.getAudioTracks()[0]
				: null;
		var remoteVideoTrack = remoteStream ? remoteStream.getVideoTracks()[0]
				: null;
		var remoteAudioTrack = remoteStream ? remoteStream.getAudioTracks()[0]
				: null;

		if (localStream) {
			kurentoTest.updateStats(peerConnection, localAudioTrack,
					"localAudio");
			kurentoTest.updateStats(peerConnection, localVideoTrack,
					"localVideo");
		} else if (remoteStream) {
			kurentoTest.updateStats(peerConnection, remoteAudioTrack,
					"remoteAudio");
			kurentoTest.updateStats(peerConnection, remoteVideoTrack,
					"remoteVideo");
		}

		for ( var key in this.rtcStats) {
			eval("this.ocrMap[time]." + key + " = \"" + this.rtcStats[key]
					+ "\";");
		}
	}
}

KurentoTest.prototype.capitalize = function(string) {
	return string.charAt(0).toUpperCase() + string.slice(1);
}

KurentoTest.prototype.getVideoTime = function(videoTagId, time) {
	if (this.ocrActive) {
		// Clock coordinates on Chrome user media synthetic video
		var sourceX = 0;
		var sourceY = 0;
		var destWidth = 440;
		var destHeight = 61;

		var video = document.getElementById(videoTagId);
		var canvas = document.getElementById("canvas");
		var context = canvas.getContext("2d");

		canvas.width = destWidth;
		canvas.height = destHeight;
		context.drawImage(video, sourceX, sourceY, destWidth, destHeight, 0, 0,
				destWidth, destHeight);

		var imgTimeBase64 = canvas.toDataURL("image/png").replace("image/png",
				"image/octet-stream");
		this.ocrMap[time] = {
			latencyMs : imgTimeBase64
		};
	}
}

KurentoTest.prototype.checkColor = function() {
	for (var i = 0; i < arguments.length; i++) {
		this.checkColorIn(arguments[i]);
	}
}

KurentoTest.prototype.checkColorIn = function(videoTagId) {
	var video = document.getElementById(videoTagId);
	var canvas = document.createElement("canvas");
	canvas.width = 1;
	canvas.height = 1;
	var canvasContext = canvas.getContext("2d");
	video.crossOrigin = "anonymous";

	// Initial color information
	this.colorInfo[videoTagId] = {};
	this.colorInfo[videoTagId].changeColor = [ 0, 0, 0, 0 ];
	this.colorInfo[videoTagId].currentColor = null;
	this.colorInfo[videoTagId].changeTime = null;

	var self = this;

	function step() {
		try {
			canvasContext.drawImage(video, self.x, self.y, 1, 1, 0, 0, 1, 1);
		} catch (e) {
			// NS_ERROR_NOT_AVAILABLE can happen in Firefox due a bug
			if (e.name != "NS_ERROR_NOT_AVAILABLE") {
				throw e;
			}
		}
		var currentColor = Array.prototype.slice.apply(canvasContext
				.getImageData(0, 0, 1, 1).data);
		self.colorInfo[videoTagId].currentColor = currentColor;

		// Convention: if exists an element with id ending in "-color" with the
		// same id than the video tag, show the current color in this element
		var colorInput = document.getElementById(videoTagId + "-color");
		if (colorInput) {
			document.getElementById(videoTagId + "-color").value = currentColor;
		}
		requestAnimationFrame(step);
	}
	requestAnimationFrame(step);
	setInterval(watchColor, this.colorCheckRate);

	function watchColor() {
		if (self.colorChanged(self.colorInfo[videoTagId].currentColor,
				self.colorInfo[videoTagId].changeColor)) {
			self.colorInfo[videoTagId].changeTime = new Date() - self.initTime;
			console.info("Detected color change in " + videoTagId
					+ " stream, from " + self.colorInfo[videoTagId].changeColor
					+ " to " + self.colorInfo[videoTagId].currentColor);
			self.colorInfo[videoTagId].changeColor = self.colorInfo[videoTagId].currentColor;

			if (videoTagId == self.latencyVideoTagsId[1] && self.firstLatency) {
				self.firstLatency = false;
			} else {
				self.latencyTime[videoTagId] = self.colorInfo[videoTagId].changeTime;
			}

			if (self.latencyVideoTagsId
					&& self.latencyTime[self.latencyVideoTagsId[0]]
					&& self.latencyTime[self.latencyVideoTagsId[1]]) {
				self.latencyTime = {};
				var latency = self.colorInfo[self.latencyVideoTagsId[1]].changeTime
						- self.colorInfo[self.latencyVideoTagsId[0]].changeTime;
				self.latency = latency;
				console.info("---> Latency " + self.latency);
			}
		}
	}
}

KurentoTest.prototype.activateLatencyControl = function(localVideoTagId,
		remoteVideoTagId) {
	this.latencyTime = {};
	this.latencyVideoTagsId = [ localVideoTagId, remoteVideoTagId ];
}

KurentoTest.prototype.getLatency = function() {
	var out = this.latency;
	this.latency = null;
	return out;
}

KurentoTest.prototype.getDataChannelState = function() {
	return document.getElementById("datachannel-state").value;
}

KurentoTest.prototype.getDataChannelMessage = function() {
	return document.getElementById("datachannel-received").value;
}

KurentoTest.prototype.colorChanged = function(expectedColor, realColor) {
	if (expectedColor && realColor) {
		var realRed = realColor[0];
		var realGreen = realColor[1];
		var realBlue = realColor[2];

		var expectedRed = expectedColor[0];
		var expectedGreen = expectedColor[1];
		var expectedBlue = expectedColor[2];

		var distance = Math.sqrt((realRed - expectedRed)
				* (realRed - expectedRed) + (realGreen - expectedGreen)
				* (realGreen - expectedGreen) + (realBlue - expectedBlue)
				* (realBlue - expectedBlue));

		return distance > this.maxColorDistance;
	} else {
		return false;
	}
}

KurentoTest.prototype.activateOutboundRtcStats = function(peerConnection) {
	this.activateRtcStats(peerConnection, "getLocalStreams", "_outbound_");
}

KurentoTest.prototype.activateInboundRtcStats = function(peerConnection) {
	this.activateRtcStats(peerConnection, "getRemoteStreams", "_inbound_");
}

KurentoTest.prototype.activateRtcStats = function(peerConnection,
		streamFunction, suffix) {
	var rate = this.rtcStatsRate;
	if (arguments.length) {
		rate = arguments[0];
	}
	kurentoTest.rtcStatsIntervalId[peerConnection + streamFunction + suffix] = setInterval(
			this.updateRtcStats, rate, eval(peerConnection), streamFunction,
			suffix);
}

KurentoTest.prototype.updateRtcStats = function(peerConnection, streamFunction,
		suffix) {
	eval("var remoteStream = peerConnection." + streamFunction + "()[0];");
	var videoTrack = remoteStream.getVideoTracks()[0];
	var audioTrack = remoteStream.getAudioTracks()[0];

	kurentoTest.updateStats(peerConnection, videoTrack, "video_peerconnection"
			+ suffix);
	kurentoTest.updateStats(peerConnection, audioTrack, "audio_peerconnection"
			+ suffix);
}

KurentoTest.prototype.updateStats = function(peerConnection, track, type) {
	peerConnection.getStats(function(stats) {
		var result = stats.result()[2];
		if (result) {
			result.names().forEach(function(name) {
				kurentoTest.rtcStats[type + name] = result.stat(name);
			});
		}
	}, track);
}

KurentoTest.prototype.stopOutboundRtcStats = function(peerConnection) {
	this.clearRtcStatsInterval(peerConnection, "getLocalStreams", "_outbound_");
}

KurentoTest.prototype.stopInboundRtcStats = function(peerConnection) {
	this.clearRtcStatsInterval(peerConnection, "getRemoteStreams", "_inbound_");
}

KurentoTest.prototype.clearRtcStatsInterval = function(peerConnection,
		streamFunction, suffix) {
	clearInterval(kurentoTest.rtcStatsIntervalId[peerConnection
			+ streamFunction + suffix]);
	this.rtcStats = {};
}

/*
 * Setters
 */
KurentoTest.prototype.setMaxColorDistance = function(maxColorDistance) {
	this.maxColorDistance = maxColorDistance;
};

KurentoTest.prototype.setColorCoordinates = function(x, y) {
	this.x = x;
	this.y = y;
};

KurentoTest.prototype.setColorCheckRate = function(colorCheckRate) {
	this.colorCheckRate = colorCheckRate;
};

/*
 * Instantiation of KurentoTest object
 */
var kurentoTest = new KurentoTest();
