/*
* (C) Copyright 2014 Kurento (http://kurento.org/)
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

const MEDIA_SERVER_HOSTNAME = location.hostname;
const ws_uri = 'ws://' + MEDIA_SERVER_HOSTNAME + ':8888/kurento';

const file_uri = 'http://files.kurento.org/video/fiwarecut.mp4'; //requires Internet connectivity
const hat_uri = 'http://files.kurento.org/imgs/mario-wings.png'; //requires Internet connectivity


window.addEventListener("load", function(event)
{
	var videoOutput = document.getElementById('videoOutput');

	var playButton = document.getElementById("playButton");
	var stopButton = document.getElementById("stopButton");

	playButton.addEventListener("click", function()
	{
		console.log("Strarting video playing ...");

		kurentoClient(ws_uri, function(error, kurentoClient)
		{
			if (error) return onError(error);

			kurentoClient.create('MediaPipeline', function(error, pipeline)
			{
				if (error) return onError(error);

				function release(event)
				{
					pipeline.release();
					videoOutput.src = "";
				}

				pipeline.create('HttpGetEndpoint', function(error, httpGetEndpoint)
				{
					if(error) return onError(error);

					pipeline.create('FaceOverlayFilter', function(error, filter)
					{
						if (error) return onError(error);

						console.log('Got FaceOverlayFilter');

						var offsetXPercent = -0.2;
						var offsetYPercent = -1.1;
						var widthPercent = 1.6;
						var heightPercent = 1.6;

						console.log('Setting overlay image');

						filter.setOverlayedImage(hat_uri, offsetXPercent, offsetYPercent,
								widthPercent,	heightPercent, function(error)
						{
							if (error) return onError(error);
						});

						filter.connect(httpGetEndpoint, function(error)
						{
							pipeline.create('PlayerEndpoint', {uri : file_uri},
									function(error, playerEndpoint)
							{
								if(error) return onError(error);

								playerEndpoint.connect(filter, function(error)
								{
									if(error) return onError(error);

									httpGetEndpoint.getUrl(function(error, url)
									{
										if(error) return onError(error);

										videoOutput.src = url;
									});

									playerEndpoint.on('EndOfStream', release);

									playerEndpoint.play(function(error)
									{
										if(error) return onError(error);

										console.log('Playing ...');
									});
								});
							});
						});
					});
				});

				stopButton.addEventListener("click", release);
			});
		});
	});
});

function onError(error) {
	console.log(error);
}
