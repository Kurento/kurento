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

QUnit.module('Composite', lifecycle);

QUnit.asyncTest('create', function () {
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
