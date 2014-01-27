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

var SessionEndPoint = require('./SessionEndPoint');


var checkType = require('../checkType');


function HttpEndPoint(objectRef, parent, pipeline, params)
{
  SessionEndPoint.call(this, objectRef, parent, pipeline);
};
HttpEndPoint.prototype.__proto__   = SessionEndPoint.prototype;
HttpEndPoint.prototype.constructor = HttpEndPoint;

HttpEndPoint.prototype.getUrl = function(callback)
{
  return this.invoke('getUrl', callback);
};


/**
 * 
 * @param {MediaHttpEndPointConstructorParams} params
 *
 * @throws {SyntaxError}
 *
 * @returns {MediaHttpEndPointConstructorParams}
 */
HttpEndPoint.checkparams = function(params)
{
  /**
   * @type {MediaHttpEndPointConstructorParams}
   */
  var result = SessionEndPoint.checkparams(params);

  // check HttpEndPoint params
  for(var key in params)
  {
    var value = params[key];

    switch(key)
    {
      case 'disconnectionTimeout':
        checkType['Integer'](key, value);
      break;

      default:
        continue;
    };

    result[key] = value;
  };
};


/**
 * @type MediaHttpEndPointConstructorParams
 *
 * @extends MediaObjectConstructorParams
 *
 * @member {Integer} disconnectionTimeout
 */


module.exports = HttpEndPoint;
