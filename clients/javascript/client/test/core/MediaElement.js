/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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

if (typeof QUnit == 'undefined') {
  QUnit = require('qunit-cli');
  QUnit.load();

  kurentoClient = require('..');

  require('./_common');
};

if (QUnit.config.prefix == undefined)
  QUnit.config.prefix = '';

QUnit.module(QUnit.config.prefix + 'MediaElement', lifecycle);

QUnit.test('setVideoFormat with Callback', function (assert) {
  assert.expect(1);

  var done = assert.async();

  this.pipeline.create(QUnit.config.prefix + 'PassThrough', function (error, passThrough) {
    if (error) return onerror(error)

    var caps = {
      codec: 'RAW',
      framerate: {
        numerator: 30,
        denominator: 1
      }
    }

    caps.framerate = kurentoClient.register.complexTypes.Fraction(caps.framerate)
    caps = kurentoClient.register.complexTypes.VideoCaps(caps)

    return passThrough.setVideoFormat(caps, function (error) {
      assert.equal(error, undefined)

      done();
    })
  })
  .catch(onerror)
});

QUnit.test('setVideoFormat with Promise', function (assert) {
  assert.expect(1);

  var done = assert.async();

  this.pipeline.create(QUnit.config.prefix + 'PassThrough').then(function (passThrough) {

    var caps = {
      codec: 'RAW',
      framerate: {
        numerator: 30,
        denominator: 1
      }
    }

    caps.framerate = kurentoClient.register.complexTypes.Fraction(caps.framerate)
    caps = kurentoClient.register.complexTypes.VideoCaps(caps)

    return passThrough.setVideoFormat(caps, function (error) {
      assert.equal(error, null)

      done();
    })
  }, function(error) {
      if (error) return onerror(error)
    })
  .catch(onerror)
});
