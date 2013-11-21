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
 * Media API for the Kurento Web SDK
 *
 * @module KwsMedia/elements
 *
 * @copyright 2014 Kurento (http://kurento.org/)
 * @license LGPL
 */

var MediaFilter = require('../MediaFilter');


/**
 * @class
 * @extends module:KwsMedia/elements~PointerDetector2Filter
 *
 * @param objectRef
 * @param parent
 * @param pipeline
 * @param {MediaHttpEndPointConstructorParams} params
 */
function PointerDetector2Filter(objectRef, parent, pipeline, params)
{
  MediaFilter.call(this, objectRef, parent, pipeline, params);
};
PointerDetector2Filter.prototype.__proto__   = MediaFilter.prototype;
PointerDetector2Filter.prototype.constructor = PointerDetector2Filter;


PointerDetector2Filter.prototype.addWindow = function(window, callback)
{
  checkType['MediaPointerDetectorWindow']('window', window);

  this.invoke('addWindow', {window: window}, callback);
};

PointerDetector2Filter.prototype.removeWindow = function(windowID, callback)
{
  this.invoke('removeWindow', {windowID: windowID}, callback);
};

PointerDetector2Filter.prototype.clearWindows = function(callback)
{
  this.invoke('clearWindows', callback);
};

PointerDetector2Filter.prototype.trackColorFromCalibrationRegion = function(callback)
{
  this.invoke('trackColorFromCalibrationRegion', callback);
};


/**
 * 
 * @param {MediaUriEndPointConstructorParams} params
 *
 * @throws {SyntaxError}
 *
 * @returns {MediaUriEndPointConstructorParams}
 */
PointerDetector2Filter.checkparams = function(params)
{
  var result = MediaFilter.checkparams(params);

  // check MediaObject params
  for(var key in params)
  {
    var value = params[key];

    switch(key)
    {
      case 'colorCalibrationRegion':
        checkType['MediaImageRegion'](key, value, true);
      break;

      case 'windowSet':
        checkType['MediaPointerDetectorWindowSet'](key, value);
      break;

      default:
        continue;
    };

    result[key] = value;
  };

  return result;
};


module.exports = PointerDetector2Filter;