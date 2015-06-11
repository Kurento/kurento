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

QUnit.module('ServerManager', lifecycle);

QUnit.asyncTest('Server manager', function (assert) {
  var self = this;

  assert.expect(1);

  self.kurento.getServerManager(function (error, server) {
      if (error) return onerror(error);

      var mediaPipeline_id;

      server.on('ObjectCreated', function (event) {
        mediaPipeline_id = event.object;
      })

      setTimeout(function () {
        self.kurento.create('MediaPipeline', function (error,
            pipeline) {
            if (error) return onerror(error);

            assert.equal(pipeline.id, mediaPipeline_id, 'ID: ' +
              pipeline.id);

            QUnit.start();
          })
          .catch(onerror)
      }, 500)
    })
    .catch(onerror)
});
