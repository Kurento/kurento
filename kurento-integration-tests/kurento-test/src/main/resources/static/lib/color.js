window.requestAnimationFrame = window.requestAnimationFrame
		|| window.mozRequestAnimationFrame
		|| window.webkitRequestAnimationFrame || window.msRequestAnimationFrame;

window.addEventListener('load', function() {
	// Local video
	var video = document.getElementById('local');
	var color = document.getElementById('localcolor');
	var localcanvas = document.createElement('CANVAS');
	checkColor(video, color, localcanvas);

	// Remote video
	var video = document.getElementById('video');
	var color = document.getElementById('color');
	var canvas = document.createElement('CANVAS');
	checkColor(video, color, canvas);
});

// Default max distance for color comparison
var maxDistance = 60;

function checkColor(video, color, canvas) {
	canvas.width = 1;
	canvas.height = 1;
	var canvasContext = canvas.getContext("2d");

	video.crossOrigin = 'anonymous';

	function step() {
		var x = document.getElementById("x").value;
		var y = document.getElementById("y").value;
		x = isNumeric(x) ? x : 0;
		y = isNumeric(y) ? y : 0;

		try {
			canvasContext.drawImage(video, x, y, 1, 1, 0, 0, 1, 1);
		} catch (e) {
			// NS_ERROR_NOT_AVAILABLE can happen in Firefox due a bug
			if (e.name != "NS_ERROR_NOT_AVAILABLE") {
				throw e;
			}
		}

		color.value = Array.prototype.slice.apply(canvasContext.getImageData(0,
				0, 1, 1).data);

		requestAnimationFrame(step);
	}
	requestAnimationFrame(step);
}

function isNumeric(n) {
	return !isNaN(parseFloat(n)) && isFinite(n);
}

function colorChanged(expectedColorStr, realColorStr) {
	var realColor = realColorStr.split(",");
	var realRed = realColor[0];
	var realGreen = realColor[1];
	var realBlue = realColor[2];

	var expectedColor = expectedColorStr.split(",");
	var expectedRed = expectedColor[0];
	var expectedGreen = expectedColor[1];
	var expectedBlue = expectedColor[2];

	var distance = Math.sqrt((realRed - expectedRed) * (realRed - expectedRed)
			+ (realGreen - expectedGreen) * (realGreen - expectedGreen)
			+ (realBlue - expectedBlue) * (realBlue - expectedBlue));

	return distance > maxDistance;
}
