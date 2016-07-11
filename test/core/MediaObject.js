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

QUnit.module(QUnit.config.prefix + 'MediaObject', lifecycle);

QUnit.test('setName with Callback', function (assert) {
  assert.expect(1);

  var done = assert.async();

  const NAME = 'MediaObjectRandomName'

  var pipeline = this.pipeline

  pipeline.setName(NAME, function (error) {
    if (error) return onerror(error)

    return pipeline.getName(function (error, name) {
      if (error) return onerror(error)

      assert.equal(name, NAME)
      done();
    })
  })
  .catch(onerror)
});

QUnit.test('setName with Promise', function (assert) {
  assert.expect(1);

  var done = assert.async();

  const NAME = 'MediaObjectRandomName'

  var pipeline = this.pipeline

  pipeline.setName(NAME).then(function () {
    return pipeline.getName().then(function(name) {
      assert.equal(name, NAME)
      done();
    }, function(error) {
      if (error) return onerror(error)
    })
  },function(error) {
      if (error) return onerror(error)
    })
  .catch(onerror)
});

