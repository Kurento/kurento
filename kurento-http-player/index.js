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

function getopts(args, opts)
{
  var result = opts.default || {};
  args.replace(
      new RegExp("([^?=&]+)(=([^&]*))?", "g"),
      function($0, $1, $2, $3) { result[$1] = $3; });

  return result;
};

var args = getopts(location.search,
{
  default:
  {
    ws_uri: 'ws://' + location.hostname + ':8888/kurento',
    as_uri: 'http://' + location.host,
    ice_servers: undefined
  }
});

if (args.ice_servers) {
  console.log("Use ICE servers: " + args.ice_servers);
  kurentoUtils.WebRtcPeer.prototype.server.iceServers = JSON.parse(args.ice_servers);
} else {
  console.log("Use freeice")
}

const file_uri = args.as_uri+'/video/fiwarecut.mp4';
const hat_uri  = args.as_uri+'/img/mario-wings.png';


window.addEventListener("load", function(event)
{
	var videoOutput = document.getElementById('videoOutput');

	var playButton = document.getElementById("playButton");
	var stopButton = document.getElementById("stopButton");

	playButton.addEventListener("click", function()
	{
		console.log("Strarting video playing ...");

		kurentoClient(args.ws_uri, function(error, kurentoClient)
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
