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


/* Sample class begin */
function Sample(file_uri) {
  var webRtcLocal
  var webRtcRemote

  this.start = function () {
    var options =
    {
      localVideo: this.tag
    }

    webRtcLocal = kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options,
    function(error)
    {
      if(error) return onError(error)

      this.generateOffer(onOffer)
    });

    var self = this;

    function onOffer(error, sdpOffer) {
      if (error) return onError(error);

      if (pipeline == null)
        return console.log("MediaPipeline is still not create")

      pipeline.create('WebRtcEndpoint', function (error, webRtc) {
        if (error) return onError(error);

        webRtc.processOffer(sdpOffer, function (error, sdpAnswer) {
          if (error) return onError(error);

          webRtc.gatherCandidates(onError);

          webRtcLocal.processAnswer(sdpAnswer);
        });

        webRtcRemote = webRtc;

        var options = {uri: file_uri}

        pipeline.create('PlayerEndpoint', options, function (error, player) {
          if (error) return onError(error);

          player.on('EndOfStream', player.play.bind(player));

          self.player = player;

          var options =
          {
            command: "capsfilter caps=video/x-raw,framerate=10/1,width=320,height=240",
            filterType: "VIDEO"
          }

          pipeline.create("GStreamerFilter", options, function (error, filter) {
            if (error) return onError(error);

            client.connect(player, filter, webRtc, function (error) {
              if (error) return onError(error);

              player.play(function (error) {
                if (error) return onError(error);

                console.log('Playing ' + file_uri);
              });
            });
          });
        });
      });
    }
  }

  this.finish = function () {
    console.log("Finishing " + file_uri);

    if (!webRtcLocal) return;

    webRtcLocal.dispose();
    webRtcLocal = null;
    this.player.stop();
    this.player.release();
    webRtcRemote.release();
  };
}


/* Sample class end */

function getopts(args, opts) {
  var result = opts.default || {};
  args.replace(
    new RegExp("([^?=&]+)(=([^&]*))?", "g"),
    function ($0, $1, $2, $3) {
      result[$1] = $3;
    });

  return result;
};

var args = getopts(location.search, {
  default: {
    ws_uri: 'ws://' + location.hostname + ':8888/kurento',
    as_uri: location.origin,
    ice_servers: undefined
  }
});

if (args.ice_servers) {
  console.log("Use ICE servers: " + args.ice_servers);
  kurentoUtils.WebRtcPeer.prototype.server.iceServers = JSON.parse(args.ice_servers);
} else {
  console.log("Use freeice")
}

kurentoClient.register('kurento-module-backgroundextractor')

var pipeline;

var videoOutput;
var webRtcPeer;
var client;

var samples = [
  new Sample(args.as_uri + '/img/fiwarecut_30.webm'),
  new Sample(args.as_uri + '/img/sintel.webm'),
  new Sample(args.as_uri + '/img/Galapagos.webm'),
  new Sample(args.as_uri + '/img/kinect.webm')
]


function setIceCandidateCallbacks(webRtcPeer, webRtcEp, onerror)
{
  webRtcPeer.on('icecandidate', function(candidate) {
    console.log("Local candidate:",candidate);

    candidate = kurentoClient.register.complexTypes.IceCandidate(candidate);

    webRtcEp.addIceCandidate(candidate, onerror)
  });

  webRtcEp.on('OnIceCandidate', function(event) {
    var candidate = event.candidate;

    console.log("Remote candidate:",candidate);

    webRtcPeer.addIceCandidate(candidate, onerror);
  });
}


window.addEventListener('load', function () {
  console = new Console();

  var video_port;
  var alphaBlending;
  var background;
  var processing = false;

  videoOutput = document.getElementById('videoOutput');
  samples[0].tag = document.getElementById('sample1');
  samples[1].tag = document.getElementById('sample2');
  samples[2].tag = document.getElementById('sample3');
  samples[3].tag = document.getElementById('sample4');

  samples[0].tag.addEventListener('click', sample1click)
  samples[1].tag.addEventListener('click', sample2click)
  samples[2].tag.addEventListener('click', sample3click)
  samples[3].tag.addEventListener('click', sample4click)

  function sample1click() {
    $('#noMore').attr('disabled', false);
    connect(samples[0].player);
  }

  function sample2click() {
    $('#noMore').attr('disabled', false);
    connect(samples[1].player);
  }

  function sample3click() {
    $('#noMore').attr('disabled', false);
    connect(samples[2].player);
  }

  function sample4click() {
    $('#noMore').attr('disabled', false);
    connect(samples[3].player);
  }

  function connect(samplePlayer) {
    if (!processing) {
      processing = true;
      background.activateProcessing(true);
    }

    if (video_port != null) {
      video_port.release();
    }

    alphaBlending.createHubPort(function (error, _video_port) {
      if (error) return onError(error);

      video_port = _video_port;
      samplePlayer.connect(video_port, function (error) {
        if (error) return onError(error);
      });
    });
  }

  var buttonStart = document.getElementById('start');
  var buttonStop = document.getElementById('stop');
  var buttonNoMore = document.getElementById('noMore');

  buttonStart.addEventListener('click', function start() {
    showSpinner(videoOutput);
    samples.forEach(function (item) {
      showSpinner(item.tag);
    });

    var options =
    {
      remoteVideo: videoOutput
    }

    webRtcPeer = kurentoUtils.WebRtcPeer.WebRtcPeerSendrecv(options, function(error)
    {
      if(error) return onError(error)

      this.generateOffer(onOffer)
    });

    $('#stop').attr('disabled', false);
    $('#start').attr('disabled', true);
    $('#noMore').attr('disabled', false);
  })
  buttonStop.addEventListener('click',stop)
  buttonNoMore.addEventListener('click', function() {
    if (video_port != null) {
      video_port.release();
    }

    background.activateProcessing(false);
    processing = false;

    $('#noMore').attr('disabled', true);
  })

  function onOffer(error, sdpOffer) {
    if (error) return onError(error);

    kurentoClient(args.ws_uri, function (error, _client) {
      if (error) return onError(error);

      client = _client

      client.create('MediaPipeline', function (error, _pipeline) {
        if (error) return onError(error);

        pipeline = _pipeline;

        samples.forEach(function (item) {
          item.start();
        });

        pipeline.create('WebRtcEndpoint', function (error, webRtc) {
          if (error) return onError(error);

          setIceCandidateCallbacks(webRtcPeer, webRtc, onError)

          webRtc.processOffer(sdpOffer, function (error, sdpAnswer) {
            if (error) return onError(error);

            webRtc.gatherCandidates(onError);

            webRtcPeer.processAnswer(sdpAnswer);
          });

          pipeline.create('AlphaBlending', function (error, _alphaBlending) {
            if (error) return onError(error);

            alphaBlending = _alphaBlending;
            alphaBlending.createHubPort(function (error, webRtc_port) {
              if (error) return onError(error);

              alphaBlending.setMaster(webRtc_port, 3, function (error) {
                if (error) return onError(error);

                console.log("Set Master Port");
              });

              pipeline.create('BackgroundExtractorFilter', function (error, _background) {
                if (error) return onError(error);

                background = _background;
                background.activateProcessing(false);

                client.connect(webRtc, background, webRtc_port, webRtc, onError);
              });
            });
          });
        });
      });
    });
  }

  $('#stop').attr('disabled', true);
  $('#start').attr('disabled', false);
  $('#noMore').attr('disabled', true);
})


function stop() {
  if (webRtcPeer) {
    webRtcPeer.dispose();
    webRtcPeer = null;
  }

  samples.forEach(function (item) {
    item.finish();
  });

  hideSpinner(videoOutput);
  samples.forEach(function (item) {
    hideSpinner(item.tag);
  });

  if (pipeline) {
    pipeline.release();
    pipeline = null;
  }

  $('#stop').attr('disabled', true);
  $('#start').attr('disabled', false);
  $('#noMore').attr('disabled', true);
}

function onError(error) {
  if(error)
  {
    console.error(error);
    stop();
  }
}


function showSpinner(tag) {
  tag.poster = 'img/transparent-1px.png';
  tag.style.background = "center transparent url('img/spinner.gif') no-repeat";
}

function hideSpinner(tag) {
  tag.src = '';
  tag.poster = './img/webrtc.png';
  tag.style.background = '';
}

/**
 * Lightbox utility (to display media pipeline image in a modal dialog)
 */
$(document).delegate('*[data-toggle="lightbox"]', 'click', function (event) {
  event.preventDefault();
  $(this).ekkoLightbox();
});
