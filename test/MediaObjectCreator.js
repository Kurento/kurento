/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

QUnit.module('MediaObjectCreator');

QUnit.test('Get unknown constructor', function (assert) {
  QUnit.expect(1);

  var MediaObject = kurentoClient.register.abstracts.MediaObject;
  var MediaObjectCreator = kurentoClient.MediaObjectCreator;

  function noop() {}

  var mediaObject = MediaObjectCreator(null, noop, noop, null, noop).createInmediate({
    type: 'dummyUnknownType'
  })

  assert.ok(mediaObject instanceof MediaObject)
});
