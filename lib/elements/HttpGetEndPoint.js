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

var HttpEndPoint = require('./HttpEndPoint');


var checkType = require('../checkType');


function HttpGetEndPoint(objectRef, parent, pipeline, params)
{
  HttpEndPoint.call(this, objectRef, parent, pipeline, params);
};
HttpGetEndPoint.prototype.__proto__   = HttpEndPoint.prototype;
HttpGetEndPoint.prototype.constructor = HttpGetEndPoint;


/**
 * 
 * @param {MediaHttpGetEndPointConstructorParams} params
 *
 * @throws {SyntaxError}
 *
 * @returns {MediaHttpGetEndPointConstructorParams}
 */
HttpGetEndPoint.checkparams = function(params)
{
  /**
   * @type {MediaHttpGetEndPointConstructorParams}
   */
  var result = HttpEndPoint.checkparams(params);

  // check HttpGetEndPoint params
  for(var key in params)
  {
    var value = params[key];

    switch(key)
    {
      case 'terminateOnEOS':
        checkType['boolean'](key, value);
      break;

      case 'profileType':
        checkType['MediaProfile'](key, value);
      break;

      default:
        continue;
    };

    result[key] = value;
  };

  return result;
};


/**
 * @type MediaHttpGetEndPointConstructorParams
 *
 * @extends MediaHttpEndPointConstructorParams
 *
 * @member {Boolean} terminateOnEOS
 * @member {String} profileType
 */


module.exports = HttpGetEndPoint;