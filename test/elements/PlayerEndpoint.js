/*
 * (C) Copyright 2013-2014 Kurento (http://kurento.org/)
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

QUnit.module('PlayerEndpoint', lifecycle);

QUnit.asyncTest('Play, Pause & Stop', function () {
  var self = this;

  QUnit.expect(4);

  self.pipeline.create('PlayerEndpoint', {
      uri: URL_SMALL
    },
    function (error, player) {
      if (error) return onerror(error);

      QUnit.notEqual(player, undefined, 'player');

      return player.play(function (error) {
        QUnit.equal(error, undefined, 'playing');

        if (error) return onerror(error);

        return player.pause(function (error) {
          QUnit.equal(error, undefined, 'paused');

          if (error) return onerror(error);

          return player.stop(function (error) {
            QUnit.equal(error, undefined, 'stoped');

            if (error) return onerror(error);

            QUnit.start();
          });
        });
      });
    })
    .catch(onerror)
});

QUnit.asyncTest('End of Stream', function (assert) {
  var self = this;

  assert.expect(2);

  var timeout = new Timeout('"PlayerEndpoint:End of Stream"',
    10 * 1000, onerror);

  function onerror(error) {
    timeout.stop();
    _onerror(error);
  };

  self.pipeline.create('PlayerEndpoint', {
    uri: URL_SMALL
  }, function (error, player) {
    if (error) return onerror(error);

    player.on('EndOfStream', function (data) {
      assert.ok(true, 'EndOfStream');

      timeout.stop();

      QUnit.start();
    })
    .catch(onerror)

    return player.play(function (error) {
      assert.equal(error, undefined, 'playing');

      if (error) return onerror(error);

      timeout.start();
    });
  })
  .catch(onerror)
});

QUnit.asyncTest('GetUri', function () {
  var self = this;

  QUnit.expect(1);

  self.pipeline.create('PlayerEndpoint', {
    uri: URL_SMALL
  }, function (error, player) {
    if (error) return onerror(error);

    return player.getUri(function (error, url) {
      if (error) return onerror(error);

      QUnit.equal(url, URL_SMALL, 'URL: ' + url);

      QUnit.start();
    });
  })
  .catch(onerror)
});

QUnit.asyncTest('Connect', function () {
  var self = this;

  QUnit.expect(4);

  self.pipeline.create('PlayerEndpoint', {
    uri: URL_SMALL
  }, function (error, player) {
    if (error) return onerror(error);

    return self.pipeline.create('RecorderEndpoint', {
      uri: URL_SMALL
    }, function (error, recorder) {
      if (error) return onerror(error);

      return player.connect(recorder, function (error) {
        QUnit.equal(error, undefined, 'connect');

        if (error) return onerror(error);

        return player.play(function (error) {
          QUnit.equal(error, undefined, 'playing');

          if (error) return onerror(error);

          return recorder.release(function (error) {
            QUnit.equal(error, undefined,
              'release recorder');

            if (error) return onerror(error);

            return player.release(function (error) {
              QUnit.equal(error, undefined,
                'release player');

              if (error) return onerror(error);

              QUnit.start();
            });
          });
        });
      });
    });
  })
  .catch(onerror)
});

QUnit.asyncTest('Connect by type', function () {
  var self = this;

  QUnit.expect(5);

  self.pipeline.create('PlayerEndpoint', {
    uri: URL_SMALL
  }, function (error, player) {
    if (error) return onerror(error);

    return self.pipeline.create('RecorderEndpoint', {
      uri: URL_SMALL
    }, function (error, recorder) {
      if (error) return onerror(error);

      return player.connect(recorder, 'AUDIO', function (error) {
        QUnit.equal(error, undefined, 'connect AUDIO');

        if (error) return onerror(error);

        return player.connect(recorder, 'VIDEO', function (error) {
          QUnit.equal(error, undefined, 'connect VIDEO');

          if (error) return onerror(error);

          return player.play(function (error) {
            QUnit.equal(error, undefined, 'play');

            if (error) return onerror(error);

            return recorder.release(function (error) {
              QUnit.equal(error, undefined, 'release recorder');

              if (error) return onerror(error);

              return player.release(function (error) {
                QUnit.equal(error, undefined, 'release player');

                if (error) return onerror(error);

                QUnit.start();
              });
            });
          });
        });
      });
    });
  })
  .catch(onerror)
});
