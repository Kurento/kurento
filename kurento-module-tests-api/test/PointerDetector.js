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
 * <li>PointerDetector
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

kurentoClient.register('kurento-module-pointerdetector')

if (QUnit.config.prefix == undefined)
  QUnit.config.prefix = '';

QUnit.module(QUnit.config.prefix + 'PointerDetector', lifecycle);

QUnit.asyncTest('Create ' + QUnit.config.prefix + 'PointerDetector', function () {
  var self = this;

  QUnit.expect(4);

  function onerror(error) {
    _onerror(error);
  };

  const PointerDetectorWindowMediaParam = kurentoClient.getComplexType('pointerdetector.PointerDetectorWindowMediaParam');
  const WindowParam = kurentoClient.getComplexType('pointerdetector.WindowParam');

  QUnit.notEqual(PointerDetectorWindowMediaParam, undefined, 'PointerDetectorWindowMediaParam');
  QUnit.notEqual(WindowParam, undefined, 'WindowParam');


   self.pipeline.create('kurento.WebRtcEndpoint', function (error, webRtcEndpoint) {
      if (error) return onerror(error);

      QUnit.notEqual(webRtcEndpoint, undefined, 'webRtcEndpoint');

      var options = {
            calibrationRegion: WindowParam({
                topRightCornerX: 5,
                topRightCornerY:5,
                width:30,
                height: 30
            })
        };

      self.pipeline.create(QUnit.config.prefix + 'PointerDetectorFilter', options, function (error, pointerDetectorFilter) {
        if (error) return onerror(error);

        QUnit.notEqual(pointerDetectorFilter, undefined, 'PointerDetectorFilter');

        QUnit.start();
      })
      .catch(onerror)

    })
    .catch(onerror)
});