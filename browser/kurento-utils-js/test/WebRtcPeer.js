/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

/**
 * {@link WebRtcPeer} test suite.
 *
 * <p>
 * Methods tested:
 * <ul>
 * <li>{@link WebRtcEndpoint#getLocalSessionDescriptor()}
 * </ul>
 * <p>
 * Events tested:
 * <ul>
 * <li>{@link WebRtcEndpoint#addMediaSessionStartListener(MediaEventListener)}
 * <li>
 * {@link HttpEndpoint#addMediaSessionTerminatedListener(MediaEventListener)}
 * </ul>
 *
 *
 * @author Jesús Leganés Combarro "piranna" (piranna@gmail.com)
 * @since 4.2.4
 *
 */

if (typeof QUnit == 'undefined') {
  QUnit = require('qunit-cli');
  QUnit.load();

  kurentoUtils = require('..');

  require('./_common');
};

var WebRtcPeer = kurentoUtils.WebRtcPeer;

var bufferizeCandidates = WebRtcPeer.bufferizeCandidates;

var WebRtcPeerRecvonly = WebRtcPeer.WebRtcPeerRecvonly;
var WebRtcPeerSendonly = WebRtcPeer.WebRtcPeerSendonly;
var WebRtcPeerSendrecv = WebRtcPeer.WebRtcPeerSendrecv;

var ac = null

function getOscillatorMedia() {
  ac = ac || new AudioContext();
  var osc = ac.createOscillator();
  var dest = ac.createMediaStreamDestination();
  osc.connect(dest);

  return dest.stream;
}

function setIceCandidateCallbacks(webRtcPeer, pc, onerror) {
  webRtcPeer.on('icecandidate', bufferizeCandidates(pc, function (error) {
    if (error) return onerror(error)

    console.debug('Received ICE candidate from WebRtcPeerRecvonly')
  }))

  pc.addEventListener('icecandidate', function (event) {
    var candidate = event.candidate
    if (candidate)
      webRtcPeer.addIceCandidate(candidate, function (error) {
        if (error) return onerror(error)

        console.debug('Received ICE candidate from PeerConnection:',
          candidate)
      })
  })
}

QUnit.module('WebRtcPeer', {
  afterEach: function () {
    if (this.webRtcPeer)
      this.webRtcPeer.dispose()

    if (this.peerConnection)
      this.peerConnection.close()
  }
});

//
// Child classes
//

QUnit.test('WebRtcPeerRecvonly', function (assert) {
  var done = assert.async();

  assert.expect(1);

  var ctx = this

  function onerror(error) {
    if (error)
      QUnit.pushFailure(error.message || error, error.stack);

    done()
  }

  var options = {
    configuration: {
      iceServers: []
    }
  }

  ctx.webRtcPeer = WebRtcPeerRecvonly(options, function (error) {
    var self = this

    if (error) return onerror(error)

    this.generateOffer(function (error, sdpOffer, processAnswer) {
      if (error) return onerror(error)

      var offer = new RTCSessionDescription({
        type: 'offer',
        sdp: sdpOffer
      });

      ctx.peerConnection = new RTCPeerConnection()

      setIceCandidateCallbacks(this, ctx.peerConnection, onerror)

      ctx.peerConnection.setRemoteDescription(offer, function () {
          var stream = getOscillatorMedia()

          ctx.peerConnection.addStream(stream)

          ctx.peerConnection.createAnswer(function (answer) {
              ctx.peerConnection.setLocalDescription(answer,
                function () {
                  processAnswer(answer.sdp, function (error) {
                    if (error) return onerror(error)

                    var stream = this.getRemoteStream()
                    assert.notEqual(stream, undefined,
                      'remote stream')

                    done()
                  })
                },
                onerror);
            },
            onerror)
        },
        onerror)
    })
  })
});

QUnit.test('WebRtcPeerSendonly', function (assert) {
  var done = assert.async();

  assert.expect(2);

  var ctx = this

  function onerror(error) {
    if (error)
      QUnit.pushFailure(error.message || error, error.stack);

    done()
  }

  var audioStream = getOscillatorMedia()

  var options = {
    audioStream: audioStream,
    configuration: {
      iceServers: []
    }
  }

  ctx.webRtcPeer = WebRtcPeerSendonly(options, function (error) {
    var self = this

    if (error) return onerror(error)

    this.generateOffer(function (error, sdpOffer, processAnswer) {
      if (error) return onerror(error)

      var stream = this.getLocalStream()
      assert.equal(stream, audioStream, 'local stream')

      var offer = new RTCSessionDescription({
        type: 'offer',
        sdp: sdpOffer
      });

      ctx.peerConnection = new RTCPeerConnection()

      setIceCandidateCallbacks(this, ctx.peerConnection, onerror)

      ctx.peerConnection.setRemoteDescription(offer, function () {
          var stream = ctx.peerConnection.getRemoteStreams()[0]
          assert.notEqual(stream, undefined, 'peer remote stream')

          ctx.peerConnection.createAnswer(function (answer) {
              ctx.peerConnection.setLocalDescription(answer,
                function () {
                  processAnswer(answer.sdp, function (error) {
                    if (error) return onerror(error)

                    done()
                  })
                },
                onerror);
            },
            onerror);
        },
        onerror)
    })
  })
});

QUnit.test('WebRtcPeerSendrecv', function (assert) {
  var done = assert.async();

  assert.expect(3);

  var ctx = this

  function onerror(error) {
    if (error)
      QUnit.pushFailure(error.message || error, error.stack);

    done()
  }

  var audioStream = getOscillatorMedia()

  var options = {
    audioStream: audioStream,
    configuration: {
      iceServers: []
    }
  }

  ctx.webRtcPeer = WebRtcPeerSendrecv(options, function (error) {
    var self = this

    if (error) return onerror(error)

    this.generateOffer(function (error, sdpOffer, processAnswer) {
      if (error) return onerror(error)

      var stream = this.getLocalStream()
      assert.equal(stream, audioStream, 'local stream')

      var offer = new RTCSessionDescription({
        type: 'offer',
        sdp: sdpOffer
      });

      ctx.peerConnection = new RTCPeerConnection()

      setIceCandidateCallbacks(this, ctx.peerConnection, onerror)

      ctx.peerConnection.setRemoteDescription(offer, function () {
          var stream = ctx.peerConnection.getRemoteStreams()[0]
          assert.notEqual(stream, undefined, 'peer remote stream')

          var stream = getOscillatorMedia();

          ctx.peerConnection.addStream(stream)

          ctx.peerConnection.createAnswer(function (answer) {
              ctx.peerConnection.setLocalDescription(answer,
                function () {
                  processAnswer(answer.sdp, function (error) {
                    if (error) return onerror(error)

                    var stream = this.getRemoteStream()
                    assert.notEqual(stream, undefined,
                      'remote stream')

                    done()
                  })
                },
                onerror);
            },
            onerror);
        },
        onerror);
    })
  })
});

//
// Methods
//

QUnit.test('processOffer', function (assert) {
  var done = assert.async();

  assert.expect(1);

  var ctx = this

  function onerror(error) {
    if (error) {
      QUnit.pushFailure(error.message || error, error.stack);
      console.debug(error)
    }

    done()
  }

  ctx.webRtcPeer = WebRtcPeerRecvonly(function (error) {
    var self = this

    if (error) return onerror(error)

    ctx.peerConnection = new RTCPeerConnection()

    setIceCandidateCallbacks(this, ctx.peerConnection, onerror)

    var stream = getOscillatorMedia()
    ctx.peerConnection.addStream(stream)

    ctx.peerConnection.createOffer(function (offer) {
        console.debug('peerConnection.createOffer')
        ctx.peerConnection.setLocalDescription(offer, function () {
            console.debug('peerConnection.setLocalDescription')
            self.processOffer(offer.sdp, function (error,
              sdpAnswer) {
              console.debug('self.processOffer')
              if (error) return onerror(error)

              var answer = new RTCSessionDescription({
                type: 'answer',
                sdp: sdpAnswer
              });

              ctx.peerConnection.setRemoteDescription(answer,
                function () {
                  var stream = self.getRemoteStream()
                  assert.notEqual(stream, undefined,
                    'remote stream')

                  done()
                },
                onerror)
            })
          },
          onerror);
      },
      onerror);
  })
});

//
// Properties
//

QUnit.test('currentFrame', function (assert) {
  var done = assert.async();

  assert.expect(2);

  var ctx = this

  function onerror(error) {
    if (error)
      QUnit.pushFailure(error.message || error, error.stack);

    done()
  }

  var video = document.getElementById('video')

  var options = {
    configuration: {
      iceServers: []
    },
    remoteVideo: video
  }

  ctx.webRtcPeer = WebRtcPeerRecvonly(options, function (error) {
    var self = this

    if (error) return onerror(error)

    this.generateOffer(function (error, sdpOffer, processAnswer) {
      if (error) return onerror(error)

      var offer = new RTCSessionDescription({
        type: 'offer',
        sdp: sdpOffer
      });

      ctx.peerConnection = new RTCPeerConnection()

      setIceCandidateCallbacks(this, ctx.peerConnection, onerror)

      ctx.peerConnection.setRemoteDescription(offer, function () {
          var mediaConstraints = {
            audio: false,
            fake: true,
            video: {
              width: {
                min: 1024,
                ideal: 1280,
                max: 1920
              },
              height: {
                min: 576,
                ideal: 720,
                max: 1080
              }
            }
          }

          getUserMedia(mediaConstraints, function (stream) {
              ctx.peerConnection.addStream(stream)

              ctx.peerConnection.createAnswer(function (
                  answer) {
                  ctx.peerConnection.setLocalDescription(
                    answer,
                    function () {
                      processAnswer(answer.sdp, function (
                        error) {
                        if (error) return onerror(error)

                        var stream = this
                          .getRemoteStream()
                        assert.notEqual(stream,
                          undefined, 'remote stream')

                        function onplaying() {
                          video.removeEventListener(
                            'playing', onplaying)

                          setTimeout(function () {
                            var currentFrame =
                              self.currentFrame

                            var x = currentFrame
                              .width /
                              2
                            var y = currentFrame
                              .height /
                              2

                            assert.notPixelEqual(
                              currentFrame, x,
                              y, 0, 0, 0, 0,
                              'playing');

                            done()
                          }, 1000)
                        }

                        video.addEventListener(
                          'playing', onplaying)
                      })
                    },
                    onerror);
                },
                onerror);
            },
            onerror)
        },
        onerror)
    })
  })
});

QUnit.test('enabled', function (assert) {
  var done = assert.async();

  assert.expect(4);

  var ctx = this

  function onerror(error) {
    if (error)
      QUnit.pushFailure(error.message || error, error.stack);

    done()
  }

  var options = {
    audioStream: getOscillatorMedia(),
    configuration: {
      iceServers: []
    }
  }

  ctx.webRtcPeer = WebRtcPeerSendonly(options, function (error, sdpoffer,
    processAnswer) {
    var self = this

    if (error) return onerror(error)

    assert.ok(this.audioEnabled, 'enabled')

    this.enabled = false
    assert.ok(!this.audioEnabled, 'disabled')

    this.enabled = true
    assert.ok(this.audioEnabled, 'enabled again')

    this.audioEnabled = false
    assert.ok(!this.enabled, 'audio disable global')

    this.dispose()
    done()
  })
});

QUnit.test('audioEnabled', function (assert) {
  var done = assert.async();

  assert.expect(3);

  var ctx = this

  function onerror(error) {
    if (error)
      QUnit.pushFailure(error.message || error, error.stack);

    done()
  }

  var options = {
    audioStream: getOscillatorMedia(),
    configuration: {
      iceServers: []
    }
  }

  ctx.webRtcPeer = WebRtcPeerSendonly(options, function (error, sdpoffer,
    processAnswer) {
    var self = this

    if (error) return onerror(error)

    var stream = this.getLocalStream()
    var track = stream.getAudioTracks()[0]

    assert.ok(track.enabled, 'enabled')

    this.audioEnabled = false
    assert.ok(!track.enabled, 'disabled')

    this.audioEnabled = true
    assert.ok(track.enabled, 'enabled again')

    this.dispose()
    done()
  })
});

QUnit.test('videoEnabled', function (assert) {
  var done = assert.async();

  assert.expect(3);

  var ctx = this

  function onerror(error) {
    if (error)
      QUnit.pushFailure(error.message || error, error.stack);

    done()
  }

  const TIMEOUT = 500; // ms

  var video = document.getElementById('video')
  var canvas = document.getElementById('canvas')
  var context = canvas.getContext('2d');

  var options = {
    configuration: {
      iceServers: []
    },
    localVideo: video,
    mediaConstraints: {
      audio: false,
      video: true,
      fake: true
    }
  }

  ctx.webRtcPeer = WebRtcPeerSendonly(options, function (error, sdpoffer,
    processAnswer) {
    var self = this

    if (error) return onerror(error)

    function onplaying() {
      video.removeEventListener('playing', onplaying)

      setTimeout(function () {
        canvas.width = video.videoWidth;
        canvas.height = video.videoHeight;

        var x = video.videoWidth / 2
        var y = video.videoHeight / 2

        context.drawImage(video, 0, 0, video.videoWidth, video
          .videoHeight)
        assert.notPixelEqual(canvas, x, y, 0, 0, 0, 0, 'enabled');

        self.videoEnabled = false

        setTimeout(function () {
          context.drawImage(video, 0, 0, video.videoWidth,
            video.videoHeight)
          assert.pixelEqual(canvas, x, y, 0, 0, 0, 255,
            'disabled');

          self.videoEnabled = true

          setTimeout(function () {
            context.drawImage(video, 0, 0, video.videoWidth,
              video.videoHeight)
            assert.notPixelEqual(canvas, x, y, 0, 0, 0,
              255, 'enabled again');

            self.dispose()
            done()
          }, TIMEOUT)
        }, TIMEOUT)
      }, TIMEOUT)
    }

    video.addEventListener('playing', onplaying)
  })
});
