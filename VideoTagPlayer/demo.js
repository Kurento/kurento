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

const ws_uri = 'ws://demo01.kurento.org:8888/thrift/ws/websocket'; //requires Internet connectivity

const file_uri = "http://files.kurento.org/video/sintel.webm"; //requires Internet connectivity

window.addEventListener("load", function(event) {
	var playButton = document.getElementById("playButton");
	playButton.addEventListener("click", startPlaying);
});

function startPlaying() {
	console.log("Strarting video playing ...");
	var videoInput = document.getElementById("videoInput");

	KwsMedia(ws_uri, function(error, kwsMedia){
		if(error) return onError(error);

		kwsMedia.create("MediaPipeline", function(error, pipeline){
			if(error) return onError(error);

			document.getElementById("stopButton").addEventListener("click", function(event){
				pipeline.release();
				videoInput.src = "";
			});

			pipeline.create("HttpGetEndpoint", function(error, httpGetEndpoint){
				if(error) return onError(error);

				pipeline.create("PlayerEndpoint", {uri : file_uri}, function(error, playerEndpoint){
					if(error) return onError(error);
					playerEndpoint.connect(httpGetEndpoint, function(error){
						if(error) return onError(error);
						httpGetEndpoint.getUrl(function(error, url){
							if(error) return onError(error);
							videoInput.src = url;
						});

						playerEndpoint.on("EndOfStream", function(event){
							pipeline.release();
							videoInput.src = "";
						});

						playerEndpoint.play(function(error){
							if(error) return onError(error);
						});
					});
				});
			});
		});
	}, onError);
}

function onError(error) {
	console.log(error);
}
