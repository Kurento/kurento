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

QUnit.module('waiting', lifecycle);

function getOnError(done) {
  return function onerror(error) {
    QUnit.pushFailure(error.message || error, error.stack);

    done();
  };
}

/**
 * Waiting 10 minutes and ask again for an object
 */
QUnit.test('Waiting 10 minutes and ask again for an object', function (assert) {
  var self = this;

  QUnit.config.testTimeout = 660000;
  QUnit.expect(2);

  var done = assert.async()
  var onerror = getOnError(done)

  var client = this.kurento
  var pipeline = this.pipeline
  var sessionId = client.sessionId;

  setTimeout(function () {
    client.getMediaobjectById(pipeline.id, function (error, pipeline_) {

      QUnit.equal(error, null)

      if (error) return onerror(error);

      QUnit.equal(pipeline.id, pipeline_.id);

      console.log("Pipeline:", pipeline.id, " pipeline_:",
        pipeline_.id)

      QUnit.config.testTimeout = 30000 * Timeout.factor;

      done();
    })
  }, 600000);

});
