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

var UriEndPoint = require('./UriEndPoint');


var checkType = require('../checkType');


/**
 * @class
 * @extends module:KwsMedia/elements~RecorderEndPoint
 *
 * @param objectRef
 * @param parent
 * @param pipeline
 * @param {MediaHttpEndPointConstructorParams} params
 */
function RecorderEndPoint(objectRef, parent, pipeline, params)
{
  UriEndPoint.call(this, objectRef, parent, pipeline, params);
};
RecorderEndPoint.prototype.__proto__   = UriEndPoint.prototype;
RecorderEndPoint.prototype.constructor = RecorderEndPoint;


/**
 * 
 * @param {RecorderEndPointConstructorParams} params
 *
 * @throws {SyntaxError}
 *
 * @returns {RecorderEndPointConstructorParams}
 */
RecorderEndPoint.checkparams = function(params)
{
  /**
   * @type {RecorderEndPointConstructorParams}
   */
  var result = UriEndPoint.checkparams(params);

  // check MediaObject params
  for(var key in params)
  {
    var value = params[key];

    switch(key)
    {
      case 'profileType':
        checkType['MediaProfile'](key, value);
      break;

      case 'stopOnEOS':
        checkType['boolean'](key, value);
      break;

      default:
        continue;
    };

    result[key] = value;
  };

  return result;
};


module.exports = RecorderEndPoint;