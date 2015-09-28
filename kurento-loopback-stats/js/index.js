/*
 * (C) Copyright 2014-2015 Kurento (http://kurento.org/)
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

function getopts(args, opts) {
  var result = opts.default || {};
  args.replace(
    new RegExp("([^?=&]+)(=([^&]*))?", "g"),
    function($0, $1, $2, $3) {
      result[$1] = decodeURI($3);
    });

  return result;
};

var args = getopts(location.search, {
  default: {
    ws_uri: 'ws://' + location.hostname + ':8888/kurento',
    ice_servers: undefined
  }
});

function setIceCandidateCallbacks(webRtcPeer, webRtcEp, onerror) {
  webRtcPeer.on('icecandidate', function(candidate) {
    console.log("Local candidate:", candidate);

    candidate = kurentoClient.register.complexTypes.IceCandidate(candidate);

    webRtcEp.addIceCandidate(candidate, onerror)
  });

  webRtcEp.on('OnIceCandidate', function(event) {
    var candidate = event.candidate;

    console.log("Remote candidate:", candidate);

    webRtcPeer.addIceCandidate(candidate, onerror);
  });
}

var webRtcPeer;
var pipeline;
var webRtcEndpoint;

window.addEventListener('load', function() {
  console = new Console();

  var videoInput = document.getElementById('videoInput');
  var videoOutput = document.getElementById('videoOutput');

  var startButton = document.getElementById("start");
  var stopButton = document.getElementById("stop");

  startButton.addEventListener("click", function() {
    showSpinner(videoInput, videoOutput);

    var options = {
      localVideo: videoInput,
      remoteVideo: videoOutput
    };

    if (args.ice_servers) {
      console.log("Use ICE servers: " + args.ice_servers);
      options.configuration = {
        iceServers : JSON.parse(args.ice_servers)
      };
    } else {
      console.log("Use freeice")
    }

    webRtcPeer = kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options, function(error) {
      if (error) return onError(error)

      this.generateOffer(onOffer)
    });

    function onOffer(error, sdpOffer) {
      if (error) return onError(error)

      kurentoClient(args.ws_uri, function(error, kurentoClient) {
        if (error) return onError(error);

        kurentoClient.create("MediaPipeline", function(error, _pipeline) {
          if (error) return onError(error);

          pipeline = _pipeline;

          //Activate the ability to gather end-to-end latency stats
          pipeline.setLatencyStats(true, function(error){
            if (error) return onError(error);
          })

          pipeline.create("WebRtcEndpoint", function(error, webRtc) {
            if (error) return onError(error);

            webRtcEndpoint = webRtc;

            setIceCandidateCallbacks(webRtcPeer, webRtc, onError)

            webRtc.processOffer(sdpOffer, function(error, sdpAnswer) {
              if (error) return onError(error);

              webRtc.gatherCandidates(onError);

              webRtcPeer.processAnswer(sdpAnswer, onError);
            });

            webRtc.connect(webRtc, function(error) {
              if (error) return onError(error);

              console.log("Loopback established");

              webRtcEndpoint.on('MediaStateChanged', function(event) {
                if (event.newState == "CONNECTED") {
                  console.log("MediaState is CONNECTED ... printing stats...")
                  activateStatsTimeout();
                }
              });
            });
          });
        });
      });
    }
  });
  stopButton.addEventListener("click", stop);
});

function activateStatsTimeout() {
  setTimeout(function() {
    if (!webRtcPeer || !pipeline) return;
    printStats();
    activateStatsTimeout();
  }, 1000);
}

function printStats() {

  getBrowserOutgoingVideoStats(webRtcPeer, function(error, stats) {
    if (error) return console.log("Warning: could not gather browser outgoing stats: " + error);

    document.getElementById('browserOutgoingSsrc').innerHTML = stats.ssrc;
    document.getElementById('browserBytesSent').innerHTML = stats.bytesSent;
    document.getElementById('browserPacketsSent').innerHTML = stats.packetsSent;
    document.getElementById('browserPliReceived').innerHTML = stats.pliCount;
    document.getElementById('browserFirReceived').innerHTML = stats.firCount;
    document.getElementById('browserNackReceived').innerHTML = stats.nackCount;
    document.getElementById('browserRtt').innerHTML = stats.roundTripTime;
    document.getElementById('browserOutboundPacketsLost').innerHTML = stats.packetsLost;
  });

  getMediaElementStats(webRtcEndpoint, 'inboundrtp', 'VIDEO', function(error, stats) {
    if (error) return console.log("Warning: could not gather webRtcEndpoing input stats: " + error);

    document.getElementById('kmsIncomingSsrc').innerHTML = stats.ssrc;
    document.getElementById('kmsBytesReceived').innerHTML = stats.bytesReceived;
    document.getElementById('kmsPacketsReceived').innerHTML = stats.packetsReceived;
    document.getElementById('kmsPliSent').innerHTML = stats.pliCount;
    document.getElementById('kmsFirSent').innerHTML = stats.firCount;
    document.getElementById('kmsNackSent').innerHTML = stats.nackCount;
    document.getElementById('kmsJitter').innerHTML = stats.jitter;
    document.getElementById('kmsPacketsLost').innerHTML = stats.packetsLost;
    document.getElementById('kmsFractionLost').innerHTML = stats.fractionLost;
    document.getElementById('kmsRembSend').innerHTML = stats.remb;
  });

  getBrowserIncomingVideoStats(webRtcPeer, function(error, stats) {
    if (error) return console.log("Warning: could not gather stats: " + error);
    document.getElementById('browserIncomingSsrc').innerHTML = stats.ssrc;
    document.getElementById('browserBytesReceived').innerHTML = stats.bytesReceived;
    document.getElementById('browserPacketsReceived').innerHTML = stats.packetsReceived;
    document.getElementById('browserPliSent').innerHTML = stats.pliCount;
    document.getElementById('browserFirSent').innerHTML = stats.firCount;
    document.getElementById('browserNackSent').innerHTML = stats.nackCount;
    document.getElementById('browserJitter').innerHTML = stats.jitter;
    document.getElementById('browserIncomingPacketLost').innerHTML = stats.packetLost;
  });

  getMediaElementStats(webRtcEndpoint, 'outboundrtp', 'VIDEO', function(error, stats){
    if (error) return console.log("Warning: could not gather webRtcEndpoing input stats: " + error);

    document.getElementById('kmsOutogingSsrc').innerHTML = stats.ssrc;
    document.getElementById('kmsBytesSent').innerHTML = stats.bytesSent;
    document.getElementById('kmsPacketsSent').innerHTML = stats.packetsSent;
    document.getElementById('kmsPliReceived').innerHTML = stats.pliCount;
    document.getElementById('kmsFirReceived').innerHTML = stats.firCount;
    document.getElementById('kmsNackReceived').innerHTML = stats.nackCount;
    document.getElementById('kmsRtt').innerHTML = stats.roundTripTime;
    document.getElementById('kmsRembReceived').innerHTML = stats.remb;
  });

  getMediaElementStats(webRtcEndpoint, 'endpoint', 'VIDEO', function(error, stats){
    if(error) return console.log("Warning: could not gather webRtcEndpoint endpoint stats: " + error);
    document.getElementById('e2eLatency').innerHTML = stats.videoE2ELatency;
  });
}


function getBrowserOutgoingVideoStats(webRtcPeer, callback) {
  if (!webRtcPeer) return callback("Cannot get stats from null webRtcPeer");
  var peerConnection = webRtcPeer.peerConnection;
  if (!peerConnection) return callback("Cannot get stats from null peerConnection");
  var localVideoStream = peerConnection.getLocalStreams()[0];
  if (!localVideoStream) return callback("Non existent local stream: cannot read stats")
  var localVideoTrack = localVideoStream.getVideoTracks()[0];
  if (!localVideoTrack) return callback("Non existent local video track: cannot read stats");

  peerConnection.getStats(function(stats) {
    var results = stats.result();
    for (var i = 0; i < results.length; i++) {
      var res = results[i];
      if (res.type != 'ssrc') continue;

      //Publish it to be compliant with W3C stats draft
      var retVal = {
        timeStamp: res.timestamp,
        //StreamStats below
        associateStatsId: res.id,
        codecId: "--",
        firCount: res.stat('googFirsReceived'),
        isRemote: false,
        mediaTrackId: res.stat('googTrackId'),
        nackCount: res.stat('googNacksReceived'),
        pliCount: res.stat('googPlisReceived'),
        sliCount: 0,
        ssrc: res.stat('ssrc'),
        transportId: res.stat('transportId'),
        //Specific outbound below
        bytesSent: res.stat('bytesSent'),
        packetsSent: res.stat('packetsSent'),
        roundTripTime: res.stat('googRtt'),
        packetsLost: res.stat('packetsLost'),
        targetBitrate: "??",
        remb: "??"
      }
      return callback(null, retVal);
    }
    return callback("Error: could not find ssrc type on track stats", null);
  }, localVideoTrack);
}

function getBrowserIncomingVideoStats(webRtcPeer, callback) {
  if (!webRtcPeer) return callback("Cannot get stats from null webRtcPeer");
  var peerConnection = webRtcPeer.peerConnection;
  if (!peerConnection) return callback("Cannot get stats from null peerConnection");
  var remoteVideoStream = peerConnection.getRemoteStreams()[0];
  if (!remoteVideoStream) return callback("Non existent remote stream: cannot read stats")
  var remoteVideoTrack = remoteVideoStream.getVideoTracks()[0];
  if (!remoteVideoTrack) return callback("Non existent remote video track: cannot read stats");

  peerConnection.getStats(function(stats) {
    var results = stats.result();
    for (var i = 0; i < results.length; i++) {
      var res = results[i];
      if (res.type != 'ssrc') continue;

      //Publish it to be compliant with W3C stats draft
      var retVal = {
        timeStamp: res.timestamp,
        //StreamStats below
        associateStatsId: res.id,
        codecId: "--",
        firCount: res.stat('googFirsSent'),
        isRemote: true,
        mediaTrackId: res.stat('googTrackId'),
        nackCount: res.stat('googNacksSent'),
        pliCount: res.stat('googPlisSent'),
        sliCount: 0,
        ssrc: res.stat('ssrc'),
        transportId: res.stat('transportId'),
        //Specific outbound below
        bytesReceived: res.stat('bytesReceived'),
        packetsReceived: res.stat('packetsReceived'),
        jitter: res.stat('googJitterBufferMs'),
        packetLost: res.stat('packetsLost'),
        remb: "??"
      }
      return callback(null, retVal);
    }
    return callback("Error: could not find ssrc type on track stats", null);
  }, remoteVideoTrack);
}

/*
Parameters:

mediaElement: valid reference of a media element.

statsType: one of
  inboundrtp
  outboundrtp
  datachannel
  element
  endpoint

mediaType: one of
  AUDIO
  VIDEO
*/
function getMediaElementStats(mediaElement, statsType, mediaType, callback){
  if (!mediaElement) return callback('Cannot get stats from null Media Element');
  if(!statsType) return callback('Cannot get stats with undefined statsType')
  if(!mediaType) mediaType = 'VIDEO'; //By default, video
  mediaElement.getStats(mediaType, function(error, statsMap){
    if(error) return callback(error);
    for(var key in statsMap){
      if(!statsMap.hasOwnProperty(key)) continue; //do not dig in prototypes properties

      stats = statsMap[key];
      if(stats.type != statsType) continue; //look for the type we want

      return callback(null, stats)
    }
    return callback('Cound not find ' +
                      statsType + ':' + mediaType +
                      ' stats in element ' + mediaElement.id);
  });
}

//Aux function used for printing stats associated to a track.
function listStats(peerConnection, webRtcEndpoint) {
  var localVideoTrack = peerConnection.getLocalStreams()[0].getVideoTracks()[0];
  var remoteVideoTrack = peerConnection.getRemoteStreams()[0].getVideoTracks()[0];

  peerConnection.getStats(function(stats) {
    var results = stats.result();

    for (var i = 0; i < results.length; i++) {
      console.log("Iterating i=" + i);
      var res = results[i];
      console.log("res.type=" + res.type);
      var names = res.names();

      for (var j = 0; j < names.length; j++) {
        var name = names[j];
        var stat = res.stat(name);
        console.log("For name " + name + " stat is " + stat);
      }
    }
  }, remoteVideoTrack);
}

function stop() {
  if (webRtcPeer) {
    webRtcPeer.dispose();
    webRtcPeer = null;
  }

  if (pipeline) {
    pipeline.release();
    pipeline = null;
  }

  hideSpinner(videoInput, videoOutput);
}

function onError(error) {
  if (error) {
    console.error(error);
    stop();
  }
}

function showSpinner() {
  for (var i = 0; i < arguments.length; i++) {
    arguments[i].poster = 'img/transparent-1px.png';
    arguments[i].style.background = "center transparent url('img/spinner.gif') no-repeat";
  }
}

function hideSpinner() {
  for (var i = 0; i < arguments.length; i++) {
    arguments[i].src = '';
    arguments[i].poster = 'img/webrtc.png';
    arguments[i].style.background = '';
  }
}

/**
 * Lightbox utility (to display media pipeline image in a modal dialog)
 */
$(document).delegate('*[data-toggle="lightbox"]', 'click', function(event) {
  event.preventDefault();
  $(this).ekkoLightbox();
});
