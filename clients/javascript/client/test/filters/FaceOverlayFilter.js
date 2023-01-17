/*
 * (C) Copyright 2013-2014 Kurento (http://kurento.org/)
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
 * {@link HttpEndpoint} test suite.
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
 * @version 1.0.0
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

QUnit.module(QUnit.config.prefix + 'FaceOverlayFilter', lifecycle);

QUnit.asyncTest('Detect face in a video with Callback', function () {
  var self = this;

  QUnit.expect(5);

  var timeout = new Timeout('"FaceOverlayFilter:Detect face in a video"',
    30 * 1000, onerror);

  function onerror(error) {
    timeout.stop();
    _onerror(error);
  };

  self.pipeline.create(QUnit.config.prefix + 'PlayerEndpoint', {
      uri: URL_POINTER_DETECTOR
    },
    function (error, player) {
      if (error) return onerror(error);

      QUnit.notEqual(player, undefined, 'player');

      self.pipeline.create(QUnit.config.prefix + 'FaceOverlayFilter', function (error, faceOverlay) {
        if (error) return onerror(error);

        QUnit.notEqual(faceOverlay, undefined, 'faceOverlay');

        return player.connect(faceOverlay, function (error) {
          QUnit.equal(error, undefined, 'connect');

          if (error) return onerror(error);

          return player.play(function (error) {
            QUnit.equal(error, undefined, 'playing');

            if (error) return onerror(error);

            timeout.start();
          });
        });
      })
      .catch(onerror)

      player.on('EndOfStream', function (data) {
        QUnit.ok(true, 'EndOfStream');

        timeout.stop();

        QUnit.start();
      });
    })
    .catch(onerror)
});

QUnit.asyncTest('Detect face in a video with Promise', function () {
  var self = this;

  QUnit.expect(3);

  var timeout = new Timeout('"FaceOverlayFilter:Detect face in a video"',
    30 * 1000, onerror);

  function onerror(error) {
    timeout.stop();
    _onerror(error);
  };

  self.pipeline.create(QUnit.config.prefix + 'PlayerEndpoint', {
      uri: URL_POINTER_DETECTOR
    }).then(function (player) {
      QUnit.notEqual(player, undefined, 'player');

      self.pipeline.create(QUnit.config.prefix + 'FaceOverlayFilter').then(function (faceOverlay) {
        QUnit.notEqual(faceOverlay, undefined, 'faceOverlay');

        return player.connect(faceOverlay).then(function () {
          return player.play().then(function () {
            timeout.start();
          });
        });
      }, function(error) {
          if (error) return onerror(error)
        })
      .catch(onerror)

      player.on('EndOfStream', function (data) {
        QUnit.ok(true, 'EndOfStream');

        timeout.stop();

        QUnit.start();
      });
    }, function(error) {
        if (error) return onerror(error)
      })
    .catch(onerror)
});
