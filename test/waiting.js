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

QUnit.module(QUnit.config.prefix + 'waiting', lifecycle);

function getOnError(done) {
  return function onerror(error) {
    QUnit.pushFailure(error.message || error, error.stack);

    done();
  };
}

/**
 * Waiting 2 minutes and ask again for an object
 */
QUnit.test('Waiting 2 minutes and ask again for an object', function (assert) {
  var self = this;

  QUnit.config.testTimeout = 132000;
  QUnit.expect(2);

  var done = assert.async()
  var onerror = getOnError(done)

  var client = this.kurento
  var pipeline = this.pipeline

  setTimeout(function () {
    client.getMediaobjectById(pipeline.id, function (error, pipeline_) {

      QUnit.equal(error, null)

      if (error) return onerror(error);

      QUnit.equal(pipeline.id, pipeline_.id);

      console.log("Pipeline:", pipeline.id, " pipeline_:",
        pipeline_.id)

      QUnit.config.testTimeout = 6000 * Timeout.factor;

      done();
    })
  }, 120000);

});
