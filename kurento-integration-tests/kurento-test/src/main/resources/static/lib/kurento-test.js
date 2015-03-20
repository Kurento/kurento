/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
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

	// Initial time
	this.initTime = new Date();
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

KurentoTest.prototype.activateRtcStats = function(peerConnection) {
	var rate = this.rtcStatsRate;
	if (arguments.length) {
		rate = arguments[0];
	}
	setInterval(this.updateRtcStats, rate, eval(peerConnection));
}

KurentoTest.prototype.updateRtcStats = function(peerConnection) {
	var remoteStream = peerConnection.getRemoteStreams()[0];
	var videoTrack = remoteStream.getVideoTracks()[0];
	var audioTrack = remoteStream.getAudioTracks()[0];

	var updateStats = function(peerConnection, track, type) {
		peerConnection.getStats(function(stats) {
			var result = stats.result()[2];
			if (result) {
				result.names().forEach(function(name) {
					kurentoTest.rtcStats[type + name] = result.stat(name);
				});
			}
		}, track);
	}

	updateStats(peerConnection, videoTrack, "video_");
	updateStats(peerConnection, audioTrack, "audio_");
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
