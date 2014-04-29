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

	function step() {
		color.value = Array.prototype.slice.apply(canvasContext.getImageData(0,
				0, 1, 1).data);
		canvasContext.drawImage(video, 0, 0, 1, 1);
		requestAnimationFrame(step);
	}
	requestAnimationFrame(step);
});
