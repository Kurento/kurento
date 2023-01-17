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
 * {@link RtpEndpoint} test suite.
 *
 * <p>
 * Methods tested:
 * <ul>
 * <li>{@link HttpEndpoint#getUrl()}
 * </ul>
 * <p>
 * Events tested:
 * <ul>
 * <li>{@link HttpEndpoint#addMediaSessionStartListener(MediaEventListener)}
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

QUnit.module(QUnit.config.prefix + 'RtpEndpoint', lifecycle);

var offer = "v=0\r\n" + "o=- 12345 12345 IN IP4 95.125.31.136\r\n" + "s=-\r\n" +
  "c=IN IP4 95.125.31.136\r\n" + "t=0 0\r\n" +
  "m=video 52126 RTP/AVP 96 97 98\r\n" + "a=rtpmap:96 H264/90000\r\n" +
  "a=rtpmap:97 MP4V-ES/90000\r\n" + "a=rtpmap:98 H263-1998/90000\r\n" +
  "a=recvonly\r\n" + "b=AS:384\r\n";

QUnit.asyncTest('Get local session descriptor with Callback', function () {
  QUnit.expect(4);

  this.pipeline.create(QUnit.config.prefix + 'RtpEndpoint', function (error, rtpEndpoint) {
    QUnit.equal(error, undefined, 'RtpEndpoint');

    if (error) return onerror(error);

    return rtpEndpoint.generateOffer(function (error) {
      QUnit.equal(error, undefined, 'generateOffer');

      if (error) return onerror(error);

      return rtpEndpoint.getLocalSessionDescriptor(function (error, sdp) {
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

  this.pipeline.create(QUnit.config.prefix + 'RtpEndpoint').then(function (rtpEndpoint) {
    QUnit.notEqual(rtpEndpoint, undefined, 'RtpEndpoint');

    return rtpEndpoint.generateOffer().then(function () {
      return rtpEndpoint.getLocalSessionDescriptor().then(function (sdp) {
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

  this.pipeline.create(QUnit.config.prefix + 'RtpEndpoint', function (error, rtpEndpoint) {
    QUnit.equal(error, undefined, 'RtpEndpoint');

    if (error) return onerror(error);

    return rtpEndpoint.processOffer(offer, function (error) {
      QUnit.equal(error, undefined, 'processOffer');

      if (error) return onerror(error);

      return rtpEndpoint.getRemoteSessionDescriptor(function (error, sdp) {
        QUnit.equal(error, undefined, 'getRemoteSessionDescriptor');

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

  this.pipeline.create(QUnit.config.prefix + 'RtpEndpoint').then(function (rtpEndpoint) {
    QUnit.notEqual(rtpEndpoint, undefined, 'RtpEndpoint');

    return rtpEndpoint.processOffer(offer).then(function () {
      return rtpEndpoint.getRemoteSessionDescriptor().then(function (sdp) {

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

  this.pipeline.create(QUnit.config.prefix + 'RtpEndpoint', function (error, rtpEndpoint) {
    QUnit.equal(error, undefined, 'RtpEndpoint');

    if (error) return onerror(error);

    return rtpEndpoint.generateOffer(function (error, offer) {
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

  this.pipeline.create(QUnit.config.prefix + 'RtpEndpoint').then(function (rtpEndpoint) {
    QUnit.notEqual(rtpEndpoint, undefined, 'RtpEndpoint');

    return rtpEndpoint.generateOffer().then(function (offer) {
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

  this.pipeline.create(QUnit.config.prefix + 'RtpEndpoint', function (error, rtpEndpoint) {
    QUnit.equal(error, undefined, 'RtpEndpoint');

    if (error) return onerror(error);

    return rtpEndpoint.processOffer(offer, function (error, answer) {
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

  this.pipeline.create(QUnit.config.prefix + 'RtpEndpoint').then(function (rtpEndpoint) {
    QUnit.notEqual(rtpEndpoint, undefined, 'RtpEndpoint');
    return rtpEndpoint.processOffer(offer).then(function (answer) {
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

  self.pipeline.create(QUnit.config.prefix + 'RtpEndpoint', function (error, rtpEndpoint) {
    QUnit.equal(error, undefined, 'RtpEndpoint');

    if (error) return onerror(error);

    return rtpEndpoint.generateOffer(function (error, offer) {
      QUnit.equal(error, undefined, 'generateOffer');

      if (error) return onerror(error);

      QUnit.notEqual(offer, undefined, 'Offer: ' + offer);

      return self.pipeline.create(QUnit.config.prefix + 'RtpEndpoint', function (error,
        rtpEndpoint2) {
        QUnit.equal(error, undefined, 'RtpEndpoint 2');

        if (error) return onerror(error);

        return rtpEndpoint2.processOffer(offer, function (error,
          answer) {
          QUnit.equal(error, undefined, 'processOffer');

          if (error) return onerror(error);

          QUnit.notEqual(answer, undefined, 'Answer: ' +
            answer);

          return rtpEndpoint.processAnswer(answer, function (error, sdp) {
            QUnit.equal(error, undefined,
              'processAnswer');

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

  self.pipeline.create(QUnit.config.prefix + 'RtpEndpoint').then(function (rtpEndpoint) {
    QUnit.notEqual(rtpEndpoint, undefined, 'RtpEndpoint');

    return rtpEndpoint.generateOffer().then(function (offer) {
      QUnit.notEqual(offer, undefined, 'Offer: ' + offer);

      return self.pipeline.create(QUnit.config.prefix + 'RtpEndpoint').then(function (rtpEndpoint2) {
        QUnit.notEqual(rtpEndpoint2, undefined, 'RtpEndpoint 2');

        return rtpEndpoint2.processOffer(offer).then(function (answer) {
          QUnit.notEqual(answer, undefined, 'Answer: ' +
            answer);

          return rtpEndpoint.processAnswer(answer).then(function (sdp) {
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

  QUnit.expect(7);

  self.pipeline.create(QUnit.config.prefix + 'PlayerEndpoint', {
      uri: URL_BARCODES
    },
    function (error, player) {
      QUnit.equal(error, undefined, 'PlayerEndpoint');

      if (error) return onerror(error);

      QUnit.notEqual(player, undefined, 'player');

      return self.pipeline.create(QUnit.config.prefix + 'RtpEndpoint', function (error, rtpEndpoint) {
        QUnit.equal(error, undefined, 'RtpEndpoint');

        if (error) return onerror(error);

        QUnit.notEqual(rtpEndpoint, undefined, 'rtpEndpoint');

        return player.connect(rtpEndpoint, 'VIDEO', function (error) {
          QUnit.equal(error, undefined, 'connect');

          if (error) return onerror(error);

          return rtpEndpoint.processOffer(offer, function (error) {
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
      QUnit.notEqual(player, undefined, 'player');

      return self.pipeline.create(QUnit.config.prefix + 'RtpEndpoint').then(function (rtpEndpoint) {
        QUnit.notEqual(rtpEndpoint, undefined, 'RtpEndpoint');

        return player.connect(rtpEndpoint, 'VIDEO').then(function () {
          return rtpEndpoint.processOffer(offer).then(function () {
            return player.play().then(function () {

              setTimeout(QUnit.start.bind(QUnit), 2 * 1000);
            }, function(error) {
              if (error) return onerror(error)
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

// QUnit.asyncTest('CampusParty simulated pipeline', function()
// {
//   var self = this;
//
//   QUnit.expect(9);
//
//   self.pipeline.create('RtpEndpoint', function(error, rtpEndpoint)
//   {
//     QUnit.equal(error, undefined, 'RtpEndpoint');
//
//     if(error) return onerror(error);
//
//     var offer = "v=0\r\n"
//               + "o=- 12345 12345 IN IP4 192.168.1.18\r\n"
//               + "s=-\r\n"
//               + "c=IN IP4 192.168.1.18\r\n"
//               + "t=0 0\r\n"
//               + "m=video 45936 RTP/AVP 96\r\n"
//               + "a=rtpmap:96 H263-1998/90000\r\n"
//               + "a=sendrecv\r\n"
//               + "b=AS:3000\r\n";
//
//     rtpEndpoint.processOffer(offer, function(error)
//     {
//       QUnit.equal(error, undefined, 'processOffer');
//
//       if(error) return onerror(error);
//
//       rtpEndpoint.getMediaSrcs('VIDEO', function(error, mediaSources)
//       {
//         QUnit.equal(error, undefined, 'getMediaSrcs');
//
//         if(error) return onerror(error);
//
//         QUnit.notEqual(mediaSources, [], 'MediaSources: '+mediaSources);
//
//         var mediaSource = mediaSources[0];
//
//         rtpEndpoint.getMediaSinks('VIDEO', function(error, mediaSinks)
//         {
//           QUnit.equal(error, undefined, 'getMediaSinks');
//
//           if(error) return onerror(error);
//
//           QUnit.notEqual(mediaSinks, [], 'MediaSinks: '+mediaSinks);
//
//           var mediaSink = mediaSinks[0];
//
//           mediaSource.connect(mediaSink, function(error)
//           {
//             QUnit.equal(error, undefined, 'connect');
//
//             if(error) return onerror(error);
//
//             self.pipeline.create('HttpGetEndpoint',
//             function(error, httpGetEndpoint)
//             {
//               QUnit.equal(error, undefined, 'HttpGetEndpoint');
//
//               if(error) return onerror(error);
//
//               rtpEndpoint.connect(httpGetEndpoint, 'VIDEO', function(error)
//               {
//                 QUnit.equal(error, undefined, 'connect VIDEO');
//
//                 if(error) return onerror(error);
//
//                 QUnit.start();
//               });
//             });
//           });
//         });
//       });
//     });
//   });
// });

// QUnit.asyncTest('Source sinks', function()
// {
//   QUnit.expect(10);
//
//   this.pipeline.create('RtpEndpoint', function(error, rtpEndpoint)
//   {
//     QUnit.equal(error, undefined, 'RtpEndpoint');
//
//     if(error) return onerror(error);
//
//     rtpEndpoint.getMediaSrcs('VIDEO', function(error, mediaSources)
//     {
//       QUnit.equal(error, undefined, 'getMediaSrcs');
//
//       if(error) return onerror(error);
//
//       QUnit.notEqual(mediaSources, [], 'MediaSources video: '+mediaSources);
//
//       rtpEndpoint.getMediaSinks('VIDEO', function(error, mediaSinks)
//       {
//         QUnit.equal(error, undefined, 'getMediaSinks');
//
//         if(error) return onerror(error);
//
//         QUnit.notEqual(mediaSinks, [], 'MediaSinks video: '+mediaSinks);
//
//         rtpEndpoint.getMediaSrcs('AUDIO', function(error, mediaSources)
//         {
//           QUnit.equal(error, undefined, 'getMediaSrcs');
//
//           if(error) return onerror(error);
//
//           QUnit.notEqual(mediaSources, [], 'MediaSources audio: '+mediaSources);
//
//           rtpEndpoint.getMediaSinks('AUDIO', function(error, mediaSinks)
//           {
//             QUnit.equal(error, undefined, 'getMediaSinks');
//
//             if(error) return onerror(error);
//
//             QUnit.notEqual(mediaSinks, [], 'MediaSinks audio: '+mediaSinks);
//
//             rtpEndpoint.release(function(error)
//             {
//               QUnit.equal(error, undefined, 'release');
//
//               if(error) return onerror(error);
//
//               QUnit.start();
//             });
//           });
//         });
//       });
//     });
//   });
// });
