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

var Http = require('./Http');


var checkType = require('../checkType');


/**
 * @class
 * @extends module:KwsMedia/elements~Http
 *
 * @param objectRef
 * @param parent
 * @param pipeline
 * @param {MediaHttpConstructorParams} params
 */
function HttpGet(objectRef, parent, pipeline, params)
{
  Http.call(this, objectRef, parent, pipeline, params);
};
HttpGet.prototype.__proto__   = Http.prototype;
HttpGet.prototype.constructor = HttpGet;


/**
 * 
 * @param {module:KwsMedia/elements~HttpGet.ConstructorParams} params
 *
 * @throws {SyntaxError}
 *
 * @returns {module:KwsMedia/elements~HttpGet.ConstructorParams}
 */
HttpGet.checkparams = function(params)
{
  /**
   * @type {module:KwsMedia/elements~HttpGet.ConstructorParams}
   */
  var result = Http.checkparams(params);

  // check HttpGet params
  for(var key in params)
  {
    var value = params[key];

    switch(key)
    {
      case 'terminateOnEOS':
        checkType('boolean', key, value);
      break;

      case 'profileType':
        checkType('MediaProfile', key, value);
      break;

      default:
        continue;
    };

    result[key] = value;
  };

  return result;
};


/**
 * @typedef module:KwsMedia/elements~HttpGet.ConstructorParams
 *
 * @property {Boolean} excludeFromGC
 * @property {external:Number} garbageCollectorPeriod
 *
 * @property {Integer} disconnectionTimeout
 *
 * @property {Boolean} terminateOnEOS
 * @property {external:String} profileType
 */


module.exports = HttpGet;