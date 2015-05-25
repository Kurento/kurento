/*
 * (C) Copyright 2015 Kurento (http://kurento.org/)
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

if (typeof QUnit == 'undefined') {
  QUnit = require('qunit-cli');
  QUnit.load();

  kurentoClient = require('..');

  require('./_common');
};

QUnit.module('MediaObject', lifecycle);

QUnit.test('setName', function (assert) {
  assert.expect(1);

  var done = assert.async();

  const NAME = 'MediaObjectRandomName'

  var pipeline = this.pipeline

  pipeline.setName(NAME, function (error) {
    if (error) return onerror(error)

    pipeline.getName(function (error, name) {
      if (error) return onerror(error)

      assert.equal(name, NAME)
      done();
    })
  })
});
