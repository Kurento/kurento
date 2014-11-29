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

/* Sample class begin */
function Sample(file_uri) {
  this.file_uri = file_uri;
}

Sample.prototype.start = function () {
  var self = this;

  self.webRtcLocal = kurentoUtils.WebRtcPeer.startRecvOnly(self.tag, function (sdpOffer) {
    if (pipeline == null) {
      console.log("MediaPipeline is still not create")
      return;
    }

    pipeline.create('WebRtcEndpoint', function (error, webRtc) {
      if (error) return onError(error);

      self.webRtcRemote = webRtc;

      pipeline.create('PlayerEndpoint', {
        uri: self.file_uri
      }, function (error, player) {
        if (error) return onError(error);

        self.player = player;

        pipeline.create("GStreamerFilter", {
          command: "capsfilter caps=video/x-raw,framerate=10/1,width=320,height=240",
          filterType: "VIDEO"
        }, function (error, filter) {
          if (error) return onError(error);

          self.filter = filter;

          player.connect(filter, function (error) {
            if (error) return onError(error);

            filter.connect(webRtc, function (error) {
              if (error) return onError(error);

              player.play(function (error) {
                if (error) return onError(error);

                console.log('Playing ' + self.file_uri);
              });

              player.on('EndOfStream', function (data) {
                player.play();
              });

              webRtc.processOffer(sdpOffer, function (error, sdpAnswer) {
                if (error) return onError(error);

                self.webRtcLocal.processSdpAnswer(sdpAnswer);
              });
            });
          });
        });
      });
    });
  }, onError);
}

Sample.prototype.finish = function () {
  console.log("Finishing " + this.file_uri);
  if (!this.webRtcLocal) {
    return;
  }

  this.webRtcLocal.dispose();
  this.webRtcLocal = null;
  this.player.stop();
  this.player.release();
  this.filter.release();
  this.webRtcRemote.release();
};
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
    as_uri: location.href,
    ice_servers: undefined
  }
});

if (args.ice_servers) {
  console.log("Use ICE servers: " + args.ice_servers);
  kurentoUtils.WebRtcPeer.prototype.server.iceServers = JSON.parse(args.ice_servers);
} else {
  console.log("Use freeice")
}

kurentoClient.register(kurentoModuleBackgroundextractor)
var pipeline;

var videoOutput;
var webRtcPeer;
var video_port;
var alphaBlending;
var background;
var processing = false;

var samples = [
  new Sample(args.as_uri + '/img/fiwarecut_30.webm'),
  new Sample(args.as_uri + '/img/sintel.webm'),
  new Sample(args.as_uri + '/img/Galapagos.webm'),
  new Sample(args.as_uri + '/img/kinect.webm')
]

window.onload = function () {
  console = new Console('console', console);
  videoOutput = document.getElementById('videoOutput');
  samples[0].tag = document.getElementById('sample1');
  samples[1].tag = document.getElementById('sample2');
  samples[2].tag = document.getElementById('sample3');
  samples[3].tag = document.getElementById('sample4');

  $('#stop').attr('disabled', true);
  $('#start').attr('disabled', false);
  $('#noMore').attr('disabled', true);
}

function start() {
  showSpinner(videoOutput);
  samples.forEach(function (item) {
    showSpinner(item.tag);
  });

  webRtcPeer = kurentoUtils.WebRtcPeer.startSendRecv(null, videoOutput, onOffer, onError);

  $('#stop').attr('disabled', false);
  $('#start').attr('disabled', true);
  $('#noMore').attr('disabled', false);
}

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

function onOffer(sdpOffer) {
  kurentoClient(args.ws_uri, function (error, kurentoClient) {
    if (error) return onError(error);

    kurentoClient.create('MediaPipeline', function (error, _pipeline) {
      if (error) return onError(error);

      pipeline = _pipeline;

      samples.forEach(function (item) {
        item.start();
      });

      pipeline.create('WebRtcEndpoint', function (error, webRtc) {
        if (error) return onError(error);

        pipeline.create('AlphaBlending', function (error, _alphaBlending) {
          if (error) return onError(error);

          alphaBlending = _alphaBlending;
          alphaBlending.createHubPort(function (error, webRtc_port) {
            if (error) return onError(error);

            pipeline.create('BackgroundExtractorFilter', function (error, _background) {
              if (error) return onError(error);

              background = _background;
              background.activateProcessing(false);

              webRtc.connect(background, function (error) {
                if (error) return onError(error);

                background.connect(webRtc_port, function (error) {
                  if (error) return onError(error);

                  webRtc_port.connect(webRtc, function (error) {
                    if (error) return onError(error);

                    alphaBlending.setMaster(webRtc_port, 3, function (error) {
                      if (error) return onError(error);

                      console.log("Set Master Port");
                    });

                    webRtc.processOffer(sdpOffer, function (error, sdpAnswer) {
                      if (error) return onError(error);

                      webRtcPeer.processSdpAnswer(sdpAnswer);
                    });
                  });
                });
              });
            });
          });
        });
      });
    });
  });
}

function onError(error) {
  console.error(error);
  stop();
}

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

function noMore() {
  if (video_port != null) {
    video_port.release();
  }

  background.activateProcessing(false);
  processing = false;

  $('#noMore').attr('disabled', true);
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
