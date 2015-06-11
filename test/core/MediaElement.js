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

QUnit.module('MediaElement', lifecycle);

QUnit.test('setVideoFormat', function (assert) {
  assert.expect(2);

  var done = assert.async();

  this.pipeline.create('PassThrough', function (error, passThrough) {
    if (error) return onerror(error)

    var caps = {
      codec: 'RAW',
      framerate: {
        numerator: 30,
        denominator: 1
      }
    }

    assert.throws(function () {
        passThrough.setVideoFormat(caps)
      },
      new SyntaxError('caps param should be a VideoCaps, not Object')
    )

    caps.framerate = kurentoClient.register.complexTypes.Fraction(caps.framerate)
    caps = kurentoClient.register.complexTypes.VideoCaps(caps)

    return passThrough.setVideoFormat(caps, function (error) {
      assert.equal(error, undefined)

      done();
    })
  })
  .catch(onerror)
});
