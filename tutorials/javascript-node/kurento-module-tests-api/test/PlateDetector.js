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
 *
 * <p>
 * Module tested:
 * <ul>
 * <li>PlateDetector
 * </ul>
 *
 *
 * @author Raúl Benítez "rbenitez" (raulbenitezmejias@gmail.com)
 * @version 1.0.0
 *
 */

if (typeof QUnit == 'undefined') {
  QUnit = require('qunit-cli');
  QUnit.load();

  kurentoClient = require('../node_modules/kurento-client/lib');

  require('./_common');
};

kurentoClient.register('kurento-module-platedetector')

if (QUnit.config.prefix == undefined)
  QUnit.config.prefix = '';

QUnit.module(QUnit.config.prefix + 'PlateDetector', lifecycle);

QUnit.asyncTest('Create ' + QUnit.config.prefix + 'PlateDetector', function () {
  var self = this;

  QUnit.expect(2);

  function onerror(error) {
    _onerror(error);
  };

   self.pipeline.create('kurento.WebRtcEndpoint', function (error, webRtcEndpoint) {
      if (error) return onerror(error);

      QUnit.notEqual(webRtcEndpoint, undefined, 'webRtcEndpoint');

      self.pipeline.create(QUnit.config.prefix + 'PlateDetectorFilter', function (error, plateDetectorFilter) {
        if (error) return onerror(error);

        QUnit.notEqual(plateDetectorFilter, undefined, 'PlateDetectorFilter');

        QUnit.start();
      })
      .catch(onerror)

    })
    .catch(onerror)
});