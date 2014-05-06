/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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
$(function(event) {
	var txtUri = $('#txtUri');
	var btnPlayStop = $('#btnPlayStop');
	var video = $('#video');
	var rangeVolume = $('#rangeVolume');
	var viewer = $('#viewer');
	var eventTypeTxt = $('#eventTypeTxt');
	var eventValueTxt = $('#eventValueTxt');
	var lastEventValue = "";

	console = new Console('console', console);

	var conn = null;

	function destroyConnection() {
		// Enable connect button
		btnPlayStop.html("Play");
		btnPlayStop.attr('disabled', false);
		txtUri.attr('disabled', false);
		conn = null;
	}

	function initConnection() {
		// Set connection success and error events
		conn.onstart = function(event) {
			console.log("Connection started");

			// Enable terminate button
			btnPlayStop.html("Stop");
			btnPlayStop.attr('disabled', false);
		};
		conn.on('terminate', function(event) {
			console.log("Connection terminated");
		});

		conn.on('remotestream', function(event) {
			console.info("RemoteStream set to " + JSON.stringify(event));
		});

		conn.on('mediaevent', function(mediaEvent) {
			// Do not send two times an event with the same value
			if (mediaEvent.data == lastEventValue)
				return;

			lastEventValue = mediaEvent.data;
			eventTypeTxt.text("Event type: " + mediaEvent.type);
			data = mediaEvent.data;
			if (data.substr(0, 4) == "http") {
				// Animate arrow to right
				var arrowRight = $('#arrowRight');
				var left = arrowRight.css("left");
				var newLeft = $(window).width()
						- (arrowRight.width() + parseInt(left));

				// [Hack] delay event to synchronize with video image
				setTimeout(function() {
					// Show event url value
					eventValueTxt.html('Event data: <a href="' + data + '">'
							+ data + '</a>');

					// Update iframe content with a fade-to-white effect
					viewer.attr('src', "about:blank");
					setTimeout(function() {
						viewer.attr('src', data);
					}, 0);

					// viewer.css('visibility', 'hidden');
					arrowRight.fadeIn('slow').animate({
						left : newLeft
					}, 1000).fadeOut('slow').animate({
						left : left
					}, 0);

					// Make appear iframe content
					// viewer.attr('src', data);

				}, 3600);
			} else {
				eventValueTxt.text("Event data: " + data);

				// Animate arrow to down
				var arrowDown = $('#arrowDown');
				var top = parseInt(arrowDown.css("top"));
				var newTop = $(window).height() - (arrowDown.height() + top);

				// Don't go backwards if the page height is too small
				if (newTop < top)
					newTop = $(window).height();

				arrowDown.fadeIn('fast').animate({
					top : newTop
				}, 'slow').fadeOut('fast').animate({
					top : top
				}, 0);

				console.info(data);
			}
		});

		conn.on('error', function(error) {
			destroyConnection();

			// Notify to the user of the error
			console.error(error.message);
		});
	}

	function playVideo() {
		// Disable terminate button
		btnPlayStop.attr('disabled', true);

		if (conn) {
			// Terminate the connection
			conn.terminate();
			console.log("Connection terminated by user");
			destroyConnection();
		}
		else {
			txtUri.attr('disabled', true);

			// Create a new connection
			var uri = txtUri.val();
			var options = {
				remoteVideoTag : 'video'
			};

			try {
				conn = new kwsContentApi.KwsContentPlayer(uri, options);
				console.log("Connection created pointing to '" + uri + "'");
				initConnection();
			} catch (error) {
				destroyConnection();
				// Notify to the user of the error
				console.error(error.message);
			}
		}
	}

	btnPlayStop.on('click', playVideo);

	txtUri.keydown(function(event) {
		var key = event.which || event.keyCode;
		if (key === 13)
			playVideo();
	});

	rangeVolume.on('change', function(event) {
		video[0].volume = rangeVolume.val();
	});
});
