/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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
 * {@link Composite} test suite.
 *
 * <p>
 * Methods tested:
 * <ul>
 * <li>{@link Composite#getLocalSessionDescriptor()}
 * </ul>
 * <p>
 * Events tested:
 * <ul>
 * <li>{@link WebRtcEndpoint#addMediaSessionStartListener(MediaEventListener)}
 * <li>
 * {@link HttpEndpoint#addMediaSessionTerminatedListener(MediaEventListener)}
 * </ul>
 *
 *
 * @author Jesús Leganés Combarro "piranna" (piranna@gmail.com)
 * @since 4.2.4
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

QUnit.module(QUnit.config.prefix + 'Composite', lifecycle);

QUnit.asyncTest('create with Callback', function () {
  QUnit.expect(4);

  this.pipeline.create('Composite', function (error, composite) {
    QUnit.equal(error, undefined, 'create composite');

    if (error) return onerror(error);

    QUnit.notEqual(composite, undefined, 'composite');

    return composite.createHubPort(function (error, hubPort) {
      QUnit.equal(error, undefined, 'createHubPort');

      if (error) return onerror(error);

      QUnit.notEqual(hubPort, undefined, 'hubPort');

      QUnit.start();
    });
  })
  .catch(onerror)
});

QUnit.asyncTest('create with Promise', function () {
  QUnit.expect(2);

  this.pipeline.create('Composite').then(function (composite) {
    QUnit.notEqual(composite, undefined, 'composite');

    return composite.createHubPort().then(function (hubPort) {
      QUnit.notEqual(hubPort, undefined, 'hubPort');

      QUnit.start();
    }, function(error) {
         if (error) return onerror(error)
      });
  }, function(error) {
      if (error) return onerror(error)
    })
  .catch(onerror)
});
