/*
 * (C) Copyright 2013-2015 Kurento (http://kurento.org/)
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

if (QUnit.config.prefix == undefined)
  QUnit.config.prefix = '';

QUnit.module(QUnit.config.prefix + 'MediaPipeline', lifecycle);

/**
 * Basic pipeline reading a video from a URL and stream it over HTTP
 */
QUnit.asyncTest('Creation with Callback', function (assert) {
  var self = this;

  assert.expect(3);

  self.pipeline.create(QUnit.config.prefix + 'PlayerEndpoint', {
    uri: URL_SMALL
  }, function (error, player) {
    if (error) return onerror(error);

    assert.notEqual(player, undefined, 'player');

    return self.pipeline.create(QUnit.config.prefix + 'RecorderEndpoint', {
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
 * Basic pipeline reading a video from a URL and stream it over HTTP
 */
QUnit.asyncTest('Creation with Promise', function (assert) {
  var self = this;

  assert.expect(2);

  self.pipeline.create(QUnit.config.prefix + 'PlayerEndpoint', {
    uri: URL_SMALL
  }).then(function(player) {
    assert.notEqual(player, undefined, 'player');

    return self.pipeline.create(QUnit.config.prefix + 'RecorderEndpoint', {
      uri: URL_SMALL
    }).then(function (recorder) {
      assert.notEqual(recorder, undefined, 'recorder');

      return player.connect(recorder).then(function () {
        QUnit.start();
      });
    }, function(error) {
      if (error) return onerror(error)
    });
  }, function(error) {
      if (error) return onerror(error)
    })
  .catch(onerror)

});

/**
 * Basic pipeline using a pseudo-syncronous API
 */
QUnit.asyncTest('Pseudo-syncronous API with Callback', function () {
  var self = this;

  QUnit.expect(0);

  var pipeline = self.pipeline;

  var player = pipeline.create(QUnit.config.prefix + 'PlayerEndpoint', {
    uri: URL_SMALL
  });
  var recorder = pipeline.create(QUnit.config.prefix + 'RecorderEndpoint', {
    uri: URL_SMALL
  });

  player.connect(recorder);

  player.release(function (error) {
    if (error) return onerror(error);

    QUnit.start();
  });
  
});
