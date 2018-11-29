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

QUnit.module(QUnit.config.prefix + 'ZBarFilter', lifecycle);

QUnit.asyncTest('Create pipeline and play video with Callback', function () {
  var self = this;

  QUnit.expect(4);

  self.pipeline.create(QUnit.config.prefix + 'PlayerEndpoint', {
    uri: URL_BARCODES
  }, function (error, player) {
    if (error) return onerror(error);

    QUnit.notEqual(player, undefined, 'player');

    return self.pipeline.create(QUnit.config.prefix + 'ZBarFilter', function (error, zbar) {
      if (error) return onerror(error);

      QUnit.notEqual(zbar, undefined, 'zbar');

      return player.connect(zbar, function (error) {
        QUnit.equal(error, undefined, 'connect');

        if (error) return onerror(error);

        return player.play(function (error) {
          QUnit.equal(error, undefined, 'play');

          if (error) return onerror(error);

          QUnit.start();
        });
      });
    });
  })
  .catch(onerror)
});

QUnit.asyncTest('Create pipeline and play video with Promise', function () {
  var self = this;

  QUnit.expect(2);

  self.pipeline.create(QUnit.config.prefix + 'PlayerEndpoint', {
    uri: URL_BARCODES
  }).then(function (player) {
    QUnit.notEqual(player, undefined, 'player');

    return self.pipeline.create(QUnit.config.prefix + 'ZBarFilter').then(function (zbar) {
      QUnit.notEqual(zbar, undefined, 'zbar');
      return player.connect(zbar).then(function () {
        return player.play().then(function () {
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
    })
  .catch(onerror)
});

QUnit.asyncTest('Detect bar-code in a video with Callback', function () {
  var self = this;

  QUnit.expect(3);

  var timeout = new Timeout('"ZBarFilter:Detect bar-code in a video"',
    15 * 1000, onerror);

  function onerror(error) {
    timeout.stop();
    _onerror(error);
  };

  self.pipeline.create(QUnit.config.prefix + 'PlayerEndpoint', {
    uri: URL_BARCODES
  }, function (error, player) {
    if (error) return onerror(error);

    return self.pipeline.create(QUnit.config.prefix + 'ZBarFilter', function (error, zbar) {
      if (error) return onerror(error);

      zbar.on('CodeFound', function (data) {
        QUnit.ok(true, 'CodeFound:' + data.value);

        timeout.stop();

        QUnit.start();
      });

      return player.connect(zbar, function (error) {
        QUnit.equal(error, undefined, 'connect');

        if (error) return onerror(error);

        return player.play(function (error) {
          QUnit.equal(error, undefined, 'play');

          if (error) return onerror(error);

          timeout.start();
        });
      });
    });
  })
  .catch(onerror)
});

QUnit.asyncTest('Detect bar-code in a video with Promise', function () {
  var self = this;

  QUnit.expect(3);

  var timeout = new Timeout('"ZBarFilter:Detect bar-code in a video"',
    15 * 1000, onerror);

  function onerror(error) {
    timeout.stop();
    _onerror(error);
  };

  self.pipeline.create(QUnit.config.prefix + 'PlayerEndpoint', {
    uri: URL_BARCODES
  }).then(function (player) {
    QUnit.notEqual(player, undefined, 'player');

    return self.pipeline.create(QUnit.config.prefix + 'ZBarFilter').then(function (zbar) {
      QUnit.notEqual(zbar, undefined, 'player');

      zbar.on('CodeFound', function (data) {
        QUnit.ok(true, 'CodeFound:' + data.value);

        timeout.stop();

        QUnit.start();
      });

      return player.connect(zbar).then(function () {
        return player.play().then(function () {
          timeout.start();
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
