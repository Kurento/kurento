/*
 * (C) Copyright 2013-2015 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the GNU Lesser General Public License (LGPL)
 * version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

/**
 * {@link MediaPipeline} basic test suite.
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

QUnit.module('MediaPipeline', lifecycle);

/**
 * Basic pipeline reading a video from a URL and stream it over HTTP
 */
QUnit.asyncTest('Creation', function (assert) {
  var self = this;

  assert.expect(3);

  self.pipeline.create('PlayerEndpoint', {
    uri: URL_SMALL
  }, function (error, player) {
    if (error) return onerror(error);

    assert.notEqual(player, undefined, 'player');

    return self.pipeline.create('RecorderEndpoint', {
      uri: URL_SMALL
    }, function (error, recorder) {
      if (error) return onerror(error);

      assert.notEqual(recorder, undefined, 'recorder');

      return player.connect(recorder, function (error) {
        assert.equal(error, undefined, 'connect');

        if (error) return onerror(error);

        QUnit.start();
      });
    });
  })
  .catch(onerror)
});

/**
 * Basic pipeline using a pseudo-syncronous API
 */
QUnit.asyncTest('Pseudo-syncronous API', function () {
  var self = this;

  QUnit.expect(0);

  var pipeline = self.pipeline;

  var player = pipeline.create('PlayerEndpoint', {
    uri: URL_SMALL
  });
  var recorder = pipeline.create('RecorderEndpoint', {
    uri: URL_SMALL
  });

  player.connect(recorder);

  player.release();

  QUnit.start();
});

/**
 * Basic pipeline using Transactional API
 */
QUnit.asyncTest('Transactional API', function () {
  var self = this;

  QUnit.expect(1);

  var player;

  self.pipeline.transaction(function () {
      player = this.create('PlayerEndpoint', {
        uri: URL_SMALL
      });
      var recorder = this.create('RecorderEndpoint', {
        uri: URL_SMALL
      });

      return player.connect(recorder);
    },
    function (error) {
      QUnit.equal(error, undefined, 'transaction ended');

      if (error) return onerror(error);

      player.release();

      QUnit.start();
    })
    .catch(onerror)
});

/**
 * Basic pipeline using transactional plain API
 */
QUnit.asyncTest('Transactional plain API', function () {
  var self = this;

  QUnit.expect(1);

  var pipeline = self.pipeline;

  var tx = pipeline.beginTransaction();
  var player = pipeline.create(tx, 'PlayerEndpoint', {
    uri: URL_SMALL
  });
  var recorder = pipeline.create(tx, 'RecorderEndpoint', {
    uri: URL_SMALL
  });

  player.connect(tx, recorder);
  tx.commit(function (error)
    //  pipeline.beginTransaction();
    //    var player  = pipeline.create('PlayerEndpoint', {uri: URL_SMALL});
    //    var httpGet = pipeline.create('HttpGetEndpoint');
    //
    //    player.connect(httpGet);
    //  pipeline.endTransaction(function(error)
    {
      QUnit.equal(error, undefined, 'transaction ended');

      if (error) return onerror(error);

      player.release();

      QUnit.start();
    });
});

/**
 * Create a transaction at beginning and send all commands on it
 */
QUnit.asyncTest('Early transaction', function () {
  var self = this;

  QUnit.expect(0);

  var pipeline = self.pipeline;

  pipeline.transaction(function () {
    var player = pipeline.create('PlayerEndpoint', {
      uri: URL_SMALL
    });
    var recorder = pipeline.create('RecorderEndpoint', {
      uri: URL_SMALL
    });

    player.connect(recorder);

    player.release();

    pipeline.release();

    QUnit.start();
  });
});
