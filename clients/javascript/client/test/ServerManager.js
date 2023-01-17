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
 * @since 5.0.5
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

QUnit.module(QUnit.config.prefix + 'ServerManager', lifecycle);

QUnit.asyncTest('Server manager getInfo with callback', function (assert) {
  var self = this;

  assert.expect(1);

  self.kurento.getServerManager(function (error, server) {
      if (error) return onerror(error);

      server.getInfo(function (error, info) {
        if (error) {
          return onerror(error)
        }
        assert.notEqual(info, undefined, 'Info: ' + info);

        QUnit.start();
      });
    })
    .catch(onerror)
});

QUnit.asyncTest('Server manager getInfo with promise', function (assert) {
  var self = this;

  assert.expect(1);

  self.kurento.getServerManager().then(function (server) {
      server.getInfo().then(function (info) {
        assert.notEqual(info, undefined, 'Info: ' + info);

        QUnit.start();
      });
    })
    .catch(onerror)
});

QUnit.asyncTest('Server manager getPipelines with promise', function (assert) {
  var self = this;

  assert.expect(1);

  self.kurento.getServerManager().then(function (server) {
      server.getPipelines().then(function (pipelines) {
        assert.notEqual(pipelines, undefined, 'Pipelines: ' +
          pipelines);

        QUnit.start();
      })
    })
    .catch(onerror)
});

QUnit.asyncTest('Server manager getPipelines with callback', function (assert) {
  var self = this;

  assert.expect(1);

  self.kurento.getServerManager(function (error, server) {
      if (error) return onerror(error);

      server.getPipelines(function (error, pipelines) {
        assert.notEqual(pipelines, undefined, 'Pipelines: ' +
          pipelines);

        QUnit.start();
      })
    })
    .catch(onerror)
});
