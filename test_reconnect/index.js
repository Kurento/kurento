#!/usr/bin/env node

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
 * @author Jesús Leganés Combarro "piranna" (piranna@gmail.com)
 * @version 1.0.0
 */

var spawn = require('child_process').spawn;

var QUnit = require('qunit-cli');
QUnit.load();

var kurentoClient = require('..');

// Get ws_port
var ws_port = ""
if (process.argv.length == 3) {
  ws_port = process.argv[2];
}

if (ws_port == "") {
  QUnit.pushFailure("The test needs a ws_port");
}

const ARGV = ['-f', './kurento.conf.json'];
const ws_uri = 'ws://127.0.0.1:' + ws_port + '/kurento'

/**
 * Manage timeouts in an object-oriented style
 */
function Timeout(id, delay, ontimeout) {
  if (!(this instanceof Timeout))
    return new Timeout(id, delay, ontimeout);

  var timeout;

  function _ontimeout(message) {
    this.stop();

    ontimeout(message);
  };

  this.start = function () {
    var delay_factor = delay * Timeout.factor;

    timeout = setTimeout(_ontimeout.bind(this), delay_factor,
      'Time out ' + id + ' (' + delay_factor + 'ms)');
  };

  this.stop = function () {
    clearTimeout(timeout);
  };
};

function getOnError(done) {
  return function onerror(error) {
    QUnit.pushFailure(error.message || error, error.stack);

    done();
  };
}

function sleep(seconds) {
  var e = new Date().getTime() + (seconds * 1000);

  while (new Date().getTime() <= e) {;
  }
}

Timeout.factor = parseFloat(QUnit.config.timeout_factor) || 1;

QUnit.config.testTimeout = 30000 * Timeout.factor;

QUnit.module('reconnect', {
  beforeEach: function () {
    var self = this;

    var options = {
      request_timeout: 5000 * Timeout.factor
    };

    this.server = spawn('kurento-media-server', ARGV)
      .on('error', onerror)

    console.log("Waiting KMS is started...")
    sleep(3)

    this.client = kurentoClient(ws_uri, options)
    this.client.create('MediaPipeline', function (error, pipeline) {
      if (error) return onerror(error);

      self.pipeline = pipeline;

      QUnit.start();
    });

    QUnit.stop();
  },

  afterEach: function () {
    this.client.close();
    this.server.kill()
  }
});

/**
 * restart the MediaServer and keep the session
 */
QUnit.test('MediaServer restarted', function (assert) {
  var self = this;

  QUnit.expect(4);

  var done = assert.async()
  var onerror = getOnError(done)

  var client = self.client
  var pipeline = self.pipeline

  var sessionId = client.sessionId;

  // restart MediaServer
  self.server.kill();
  self.server.on('exit', function (code, signal) {
    assert.equal(code, undefined, 'MediaServer killed');

    client._resetCache()

    self.server = spawn('kurento-media-server', ARGV)
      .on('error', onerror)

    console.log("Waiting KMS is started again...")
    sleep(3)

    client.getMediaobjectById(pipeline.id, function (error, mediaObject) {
      assert.notEqual(error, undefined);
      assert.strictEqual(error.code, 40101);

      assert.strictEqual(client.sessionId, sessionId);

      done();
    })
  })
});

/**
 * All objects are Invalid after the MediaServer got down
 */
QUnit.test('MediaServer closed, client disconnected', function (assert) {
  var self = this;

  QUnit.expect(2);

  var done = assert.async()
  var onerror = getOnError(done)

  var client = self.client
  var pipeline = self.pipeline

  // stop MediaServer
  self.server.kill();
  self.server.on('exit', function (code, signal) {
    client.once('disconnect', function (error) {
      assert.notEqual(error, undefined);

      assert.throws(function () {
          client.sessionId
        },
        new SyntaxError('Client has been disconnected'));

      done();
    });
  })
});
