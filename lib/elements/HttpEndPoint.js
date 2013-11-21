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

var SessionEndPoint = require('./SessionEndPoint');


var checkType = require('../checkType');


/**
 * @class
 * @extends module:KwsMedia/elements~SessionEndPoint
 *
 * @param objectRef
 * @param parent
 * @param pipeline
 * @param {module:KwsMedia/elements~HttpEndPoint.ConstructorParams} params
 */
function HttpEndPoint(objectRef, parent, pipeline, params)
{
  SessionEndPoint.call(this, objectRef, parent, pipeline);
};
HttpEndPoint.prototype.__proto__   = SessionEndPoint.prototype;
HttpEndPoint.prototype.constructor = HttpEndPoint;

/**
 * 
 *
 * @param callback
 *
 * @returns {external:String}
 */
HttpEndPoint.prototype.getUrl = function(callback)
{
  return this.invoke('getUrl', callback);
};


/**
 * 
 * @param {module:KwsMedia/elements~HttpEndPoint.ConstructorParams} params
 *
 * @throws {SyntaxError}
 *
 * @returns {module:KwsMedia/elements~HttpEndPoint.ConstructorParams}
 */
HttpEndPoint.checkparams = function(params)
{
  /**
   * @type {module:KwsMedia/elements~HttpEndPoint.ConstructorParams}
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
 * @typedef module:KwsMedia/elements~HttpEndPoint.ConstructorParams
 *
 * @property {Boolean} excludeFromGC
 * @property {external:Number} garbageCollectorPeriod
 *
 * @property {Integer} disconnectionTimeout
 */


module.exports = HttpEndPoint;