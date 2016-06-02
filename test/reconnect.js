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

QUnit.module(QUnit.config.prefix + 'reconnect', lifecycle);

function getOnError(done) {
  return function onerror(error) {
    QUnit.pushFailure(error.message || error, error.stack);

    done();
  };
}

/**
 * Close the connection and keep working
 */
QUnit.test('Continue after network error', function (assert) {
  var self = this;

  QUnit.expect(13);

  var done = assert.async()
  var onerror = getOnError(done)

  var client = this.kurento
  var pipeline = this.pipeline
  var sessionId = client.sessionId;

  assert.notEqual(sessionId, undefined);

  pipeline.create(QUnit.config.prefix + 'PlayerEndpoint', {
      uri: URL_SMALL
    }, function (error, player) {
      if (error) return onerror(error);

      return pipeline.create(QUnit.config.prefix + 'RecorderEndpoint', {
          uri: URL_SMALL
        },
        function (error, recorder) {
          if (error) return onerror(error);

          return player.connect(recorder, function (error) {
            if (error) return onerror(error);

            // End connection and wait for a new one
            var old_connection = client._re._connection;
            old_connection.end();

            client._resetCache()

            client._re.once('connect', function (con) {
              assert.notStrictEqual(con, old_connection);

              assert.strictEqual(client.sessionId, sessionId,
                'sessionId=' + client.sessionId);

              var ids = [pipeline.id, player.id, recorder.id]

              client.getMediaobjectById(ids, function (error,
                  mediaObjects) {
                  if (error) return onerror(error);

                  assert.strictEqual(mediaObjects.length, 3);

                  assert.strictEqual(mediaObjects[0].id,
                    pipeline.id);
                  assert.strictEqual(mediaObjects[1].id,
                    player.id);
                  assert.strictEqual(mediaObjects[2].id,
                    recorder.id);

                  assert.strictEqual(mediaObjects[0].__module__,
                    pipeline.__module__);
                  assert.strictEqual(mediaObjects[1].__module__,
                    player.__module__);
                  assert.strictEqual(mediaObjects[2].__module__,
                    recorder.__module__);

                  assert.strictEqual(mediaObjects[0].__type__,
                    pipeline.__type__);
                  assert.strictEqual(mediaObjects[1].__type__,
                    player.__type__);
                  assert.strictEqual(mediaObjects[2].__type__,
                    recorder.__type__);

                  return player.play(function (error) {
                    if (error) return onerror(error);

                    done();
                  })
                })
                .catch(onerror)
            });
          });
        });
    })
    .catch(onerror)
});

// Re-send requests on network error
