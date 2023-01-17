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
 * <li>CrowDetector
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

kurentoClient.register('kurento-module-crowddetector')

if (QUnit.config.prefix == undefined)
  QUnit.config.prefix = '';

QUnit.module(QUnit.config.prefix + 'CrowdDetector', lifecycle);

QUnit.asyncTest('Create ' + QUnit.config.prefix + 'CrowdDetector', function () {
  var self = this;

  QUnit.expect(5);

  function onerror(error) {
    _onerror(error);
  };

  const RegionOfInterest = kurentoClient.getComplexType(QUnit.config.prefix + 'RegionOfInterest');
  const RegionOfInterestConfig = kurentoClient.getComplexType(QUnit.config.prefix + 'RegionOfInterestConfig');
  const RelativePoint = kurentoClient.getComplexType(QUnit.config.prefix + 'RelativePoint');

  QUnit.notEqual(RegionOfInterest, undefined, 'RegionOfInterest');
  QUnit.notEqual(RegionOfInterestConfig, undefined, 'RegionOfInterestConfig');
  QUnit.notEqual(RelativePoint, undefined, 'RelativePoint');


   self.pipeline.create('WebRtcEndpoint', function (error, webRtcEndpoint) {
      if (error) return onerror(error);

      QUnit.notEqual(webRtcEndpoint, undefined, 'webRtcEndpoint');

      var options = {
          rois: [
            RegionOfInterest({
              id: 'roi1',
              points: [
                RelativePoint({x: 0  , y: 0  }),
                RelativePoint({x: 0.5, y: 0  }),
                RelativePoint({x: 0.5, y: 0.5}),
                RelativePoint({x: 0  , y: 0.5})
              ],
              regionOfInterestConfig: RegionOfInterestConfig({
                occupancyLevelMin: 10,
                occupancyLevelMed: 35,
                occupancyLevelMax: 65,
                occupancyNumFramesToEvent: 5,
                fluidityLevelMin: 10,
                fluidityLevelMed: 35,
                fluidityLevelMax: 65,
                fluidityNumFramesToEvent: 5,
                sendOpticalFlowEvent: false,
                opticalFlowNumFramesToEvent: 3,
                opticalFlowNumFramesToReset: 3,
                opticalFlowAngleOffset: 0
              })
            })
          ]
        }

      self.pipeline.create(QUnit.config.prefix + 'CrowdDetectorFilter', options, function (error, crowdDetectorFilter) {
        if (error) return onerror(error);

        QUnit.notEqual(crowdDetectorFilter, undefined, 'CrowdDetectorFilter');

        QUnit.start();
      })
      .catch(onerror)

    })
    .catch(onerror)
});