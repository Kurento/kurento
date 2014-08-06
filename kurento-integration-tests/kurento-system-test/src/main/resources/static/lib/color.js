window.requestAnimationFrame = window.requestAnimationFrame
		|| window.mozRequestAnimationFrame
		|| window.webkitRequestAnimationFrame || window.msRequestAnimationFrame;

window.addEventListener('load', function() {
	var video = document.getElementById('video');
	var color = document.getElementById('color');
	var canvas = document.createElement('CANVAS');
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
});

function isNumeric(n) {
	return !isNaN(parseFloat(n)) && isFinite(n);
}
