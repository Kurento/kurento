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
 * New node file
 */

var MediaFilter = require('../MediaFilter');


function PointerDetectorFilter(objectRef, parent, pipeline, params)
{
  MediaFilter.call(this, objectRef, parent, pipeline, params);
};
PointerDetectorFilter.prototype.__proto__   = MediaFilter.prototype;
PointerDetectorFilter.prototype.constructor = PointerDetectorFilter;


PointerDetectorFilter.prototype.addWindow = function(window, callback)
{
  checkType['MediaPointerDetectorWindow']('window', window);

  this.invoke('addWindow', {window: window}, callback);
};

PointerDetectorFilter.prototype.removeWindow = function(windowID, callback)
{
  this.invoke('removeWindow', {windowID: windowID}, callback);
};

PointerDetectorFilter.prototype.clearWindows = function(callback)
{
  this.invoke('clearWindows', callback);
};

PointerDetectorFilter.prototype.trackColorFromCalibrationRegion = function(callback)
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
PointerDetectorFilter.checkparams = function(params)
{
  var result = MediaFilter.checkparams(params);

  // check MediaObject params
  for(var key in params)
  {
    var value = params[key];

    switch(key)
    {
      case 'windows':
        Set_MediaPointerDetectorWindow(key, value);
      break;

      default:
        continue;
    };

    result[key] = value;
  };

  return result;
};


module.exports = PointerDetectorFilter;