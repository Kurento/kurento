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


var checkType = require('../checkType');


function ChromaFilter(objectRef, parent, pipeline, params)
{
  MediaFilter.call(this, objectRef, parent, pipeline, params);
};
ChromaFilter.prototype.__proto__   = MediaFilter.prototype;
ChromaFilter.prototype.constructor = ChromaFilter;


ChromaFilter.prototype.setBackground = function(backgroundImage, callback)
{
  checkType['KmsMediaChromaBackgroundImage']('backgroundImage', backgroundImage);

  this.invoke('setBackground', {backgroundImage: backgroundImage}, callback);
};

ChromaFilter.prototype.unsetBackground = function(callback)
{
  this.invoke('unsetBackground', callback);
};


/**
 * 
 * @param {MediaUriEndPointConstructorParams} params
 *
 * @throws {SyntaxError}
 *
 * @returns {MediaUriEndPointConstructorParams}
 */
ChromaFilter.checkparams = function(params)
{
  var result = MediaFilter.checkparams(params);

  // check MediaObject params
  for(var key in params)
  {
    var value = params[key];

    switch(key)
    {
      case 'calibrationArea':
        checkType['KmsMediaChromaColorCalibrationArea'](key, value, true);
      break;

      case 'backgroundImage':
        checkType['KmsMediaChromaBackgroundImage'](key, value);
      break;

      default:
        continue;
    };

    result[key] = value;
  };

  return result;
};


module.exports = ChromaFilter;