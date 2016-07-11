/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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
 * {@link WebRtcEndpoint} test suite.
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

  kurentoClient = require('..');

  require('./_common');
  require('./_proxy');
};

if (QUnit.config.prefix == undefined)
  QUnit.config.prefix = '';

QUnit.module(QUnit.config.prefix + 'WebRtcEndpoint', lifecycle);

var offer = "v=0\r\n" + "o=- 12345 12345 IN IP4 95.125.31.136\r\n" + "s=-\r\n" +
  "c=IN IP4 95.125.31.136\r\n" + "t=0 0\r\n" +
  "m=video 52126 RTP/AVP 96 97 98\r\n" + "a=rtpmap:96 H264/90000\r\n" +
  "a=rtpmap:97 MP4V-ES/90000\r\n" + "a=rtpmap:98 H263-1998/90000\r\n" +
  "a=recvonly\r\n" + "b=AS:384\r\n";

QUnit.asyncTest('Get local session descriptor with Callback', function () {
  QUnit.expect(4);

  this.pipeline.create(QUnit.config.prefix + 'WebRtcEndpoint', function (error, webRtcEndpoint) {
    QUnit.equal(error, undefined, 'WebRtcEndpoint');

    if (error) return onerror(error);

    return webRtcEndpoint.generateOffer(function (error) {
      QUnit.equal(error, undefined, 'generateOffer');

      if (error) return onerror(error);

      return webRtcEndpoint.getLocalSessionDescriptor(function (error, sdp) {
        QUnit.equal(error, undefined, 'getLocalSessionDescriptor');

        if (error) return onerror(error);

        QUnit.notEqual(sdp, undefined, 'SDP: ' + sdp);

        QUnit.start();
      });
    });
  })
  .catch(onerror)
});

QUnit.asyncTest('Get local session descriptor with Promise', function () {
  QUnit.expect(2);

  this.pipeline.create(QUnit.config.prefix + 'WebRtcEndpoint').then(function (webRtcEndpoint) {
    QUnit.notEqual(webRtcEndpoint, undefined, 'WebRtcEndpoint');
    
    return webRtcEndpoint.generateOffer().then(function () {
      return webRtcEndpoint.getLocalSessionDescriptor().then(function (sdp) {
        QUnit.notEqual(sdp, undefined, 'SDP: ' + sdp);

        QUnit.start();
      }, function(error) {
          if (error) return onerror(error)
      });
    }, function(error) {
          if (error) return onerror(error)
      });
  })
  .catch(onerror)
});

QUnit.asyncTest('Get remote session descriptor with Callback', function () {
  QUnit.expect(4);

  this.pipeline.create(QUnit.config.prefix + 'WebRtcEndpoint', function (error, webRtcEndpoint) {
    QUnit.equal(error, undefined, 'WebRtcEndpoint');

    if (error) return onerror(error);

    return webRtcEndpoint.processOffer(offer, function (error) {
      QUnit.equal(error, undefined, 'processOffer');

      if (error) return onerror(error);

      return webRtcEndpoint.getRemoteSessionDescriptor(function (error, sdp) {
        QUnit.equal(error, undefined,
          'getRemoteSessionDescriptor');

        if (error) return onerror(error);

        QUnit.notEqual(sdp, undefined, 'SDP: ' + sdp);

        QUnit.start();
      });
    });
  })
  .catch(onerror)
});

QUnit.asyncTest('Get remote session descriptor with Promise', function () {
  QUnit.expect(2);

  this.pipeline.create(QUnit.config.prefix + 'WebRtcEndpoint').then(function (webRtcEndpoint) {
    QUnit.notEqual(webRtcEndpoint, undefined, 'WebRtcEndpoint');

    return webRtcEndpoint.processOffer(offer).then(function () {
      return webRtcEndpoint.getRemoteSessionDescriptor().then(function (sdp) {
        QUnit.notEqual(sdp, undefined, 'SDP: ' + sdp);

        QUnit.start();
      }, function(error) {
          if (error) return onerror(error)
      });
    }, function(error) {
          if (error) return onerror(error)
      });
  })
  .catch(onerror)
});

QUnit.asyncTest('Generate offer with Callback', function () {
  QUnit.expect(3);

  this.pipeline.create(QUnit.config.prefix + 'WebRtcEndpoint', function (error, webRtcEndpoint) {
    QUnit.equal(error, undefined, 'WebRtcEndpoint');

    if (error) return onerror(error);

    return webRtcEndpoint.generateOffer(function (error, offer) {
      QUnit.equal(error, undefined, 'generateOffer');

      if (error) return onerror(error);

      QUnit.notEqual(offer, undefined, 'Offer: ' + offer);

      QUnit.start();
    });
  })
  .catch(onerror)
});

QUnit.asyncTest('Generate offer with Promise', function () {
  QUnit.expect(2);

  this.pipeline.create(QUnit.config.prefix + 'WebRtcEndpoint').then(function (webRtcEndpoint) {
    QUnit.notEqual(webRtcEndpoint, undefined, 'WebRtcEndpoint');

    return webRtcEndpoint.generateOffer().then(function (offer) {
      QUnit.notEqual(offer, undefined, 'Offer: ' + offer);

      QUnit.start();
    }, function(error) {
          if (error) return onerror(error)
      });
  }, function(error) {
          if (error) return onerror(error)
      })
  .catch(onerror)
});

QUnit.asyncTest('Process offer with Callback', function () {
  QUnit.expect(3);

  this.pipeline.create(QUnit.config.prefix + 'WebRtcEndpoint', function (error, webRtcEndpoint) {
    QUnit.equal(error, undefined, 'WebRtcEndpoint');

    if (error) return onerror(error);

    return webRtcEndpoint.processOffer(offer, function (error, answer) {
      QUnit.equal(error, undefined, 'processOffer');

      if (error) return onerror(error);

      QUnit.notEqual(answer, undefined, 'Answer: ' + answer);

      QUnit.start();
    });
  })
  .catch(onerror)
});

QUnit.asyncTest('Process offer with Promise', function () {
  QUnit.expect(2);

  this.pipeline.create(QUnit.config.prefix + 'WebRtcEndpoint').then(function (webRtcEndpoint) {
    QUnit.notEqual(webRtcEndpoint, undefined, 'WebRtcEndpoint');
    return webRtcEndpoint.processOffer(offer).then(function (answer) {
      QUnit.notEqual(answer, undefined, 'Answer: ' + answer);

      QUnit.start();
    }, function(error) {
          if (error) return onerror(error)
      });
  }, function(error) {
          if (error) return onerror(error)
      })
  .catch(onerror)
});

QUnit.asyncTest('Process answer with Callback', function () {
  var self = this;

  QUnit.expect(8);

  self.pipeline.create(QUnit.config.prefix + 'WebRtcEndpoint', function (error, webRtcEndpoint) {
    QUnit.equal(error, undefined, 'WebRtcEndpoint');

    if (error) return onerror(error);

    return webRtcEndpoint.generateOffer(function (error, offer) {
      QUnit.equal(error, undefined, 'generateOffer');

      if (error) return onerror(error);

      QUnit.notEqual(offer, undefined, 'Offer: ' + offer);

      return self.pipeline.create(QUnit.config.prefix + 'WebRtcEndpoint', function (error,
        webRtcEndpoint2) {
        QUnit.equal(error, undefined, 'WebRtcEndpoint 2');

        if (error) return onerror(error);

        return webRtcEndpoint2.processOffer(offer, function (error, answer) {
          QUnit.equal(error, undefined, 'processOffer');

          if (error) return onerror(error);

          QUnit.notEqual(answer, undefined, 'Answer: ' + answer);

          return webRtcEndpoint.processAnswer(answer, function (error, sdp) {
            QUnit.equal(error, undefined, 'processAnswer');

            if (error) return onerror(error);

            QUnit.notEqual(sdp, undefined, 'SDP: ' + sdp);

            QUnit.start();
          });
        });
      });
    });
  })
  .catch(onerror)
});

QUnit.asyncTest('Process answer with Promise', function () {
  var self = this;

  QUnit.expect(5);

  self.pipeline.create(QUnit.config.prefix + 'WebRtcEndpoint').then(function (webRtcEndpoint) {
    QUnit.notEqual(webRtcEndpoint, undefined, 'WebRtcEndpoint');

    return webRtcEndpoint.generateOffer().then(function (offer) {
      QUnit.notEqual(offer, undefined, 'Offer: ' + offer);

      return self.pipeline.create(QUnit.config.prefix + 'WebRtcEndpoint').then(function (webRtcEndpoint2) {
        QUnit.notEqual(webRtcEndpoint2, undefined, 'WebRtcEndpoint 2');

        return webRtcEndpoint2.processOffer(offer).then(function (answer) {
          QUnit.notEqual(answer, undefined, 'Answer: ' + answer);

          return webRtcEndpoint.processAnswer(answer).then(function (sdp) {
            QUnit.notEqual(sdp, undefined, 'SDP: ' + sdp);

            QUnit.start();
          }, function(error) {
          if (error) return onerror(error)
          });
        }, function(error) {
          if (error) return onerror(error)
        });
      }, function(error) {
          if (error) return onerror(error)
      });
    }, function(error) {
          if (error) return onerror(error)
      });
  }, function(error) {
          if (error) return onerror(error)
    })
  .catch(onerror)
});

QUnit.asyncTest('RtpEndpoint simulating Android SDP with Callback', function () {
  var self = this;

  QUnit.expect(5);

  self.pipeline.create(QUnit.config.prefix + 'PlayerEndpoint', {
      uri: URL_BARCODES
    },
    function (error, player) {
      QUnit.equal(error, undefined, 'PlayerEndpoint');

      if (error) return onerror(error);

      return self.pipeline.create(QUnit.config.prefix + 'WebRtcEndpoint', function (error,
        webRtcEndpoint) {
        QUnit.equal(error, undefined, 'WebRtcEndpoint');

        if (error) return onerror(error);

        return player.connect(webRtcEndpoint, 'VIDEO', function (error) {
          QUnit.equal(error, undefined, 'connect');

          if (error) return onerror(error);

          return webRtcEndpoint.processOffer(offer, function (error) {
            QUnit.equal(error, undefined, 'processOffer');

            if (error) return onerror(error);

            return player.play(function (error) {
              QUnit.equal(error, undefined, 'play');

              if (error) return onerror(error);

              setTimeout(QUnit.start.bind(QUnit), 2 * 1000);
            })
          });
        });
      });
    })
    .catch(onerror)
});

QUnit.asyncTest('RtpEndpoint simulating Android SDP with Promise', function () {
  var self = this;

  QUnit.expect(2);

  self.pipeline.create(QUnit.config.prefix + 'PlayerEndpoint', {
      uri: URL_BARCODES
    }).then(function (player) {
      QUnit.notEqual(player, undefined, 'PlayerEndpoint');

      return self.pipeline.create(QUnit.config.prefix + 'WebRtcEndpoint').then(function (webRtcEndpoint) {
        QUnit.notEqual(webRtcEndpoint, undefined, 'WebRtcEndpoint');

        return player.connect(webRtcEndpoint, 'VIDEO').then(function () {
          return webRtcEndpoint.processOffer(offer).then(function () {
            return player.play().then(function () {
              setTimeout(QUnit.start.bind(QUnit), 2 * 1000);
            })
          }, function(error) {
            if (error) return onerror(error)
          });
        }, function(error) {
            if (error) return onerror(error)
          });
      }, function(error) {
          if (error) return onerror(error)
        });
    }, function(error) {
         if (error) return onerror(error)
      })
    .catch(onerror)
});
