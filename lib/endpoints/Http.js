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

var Session = require('./Session');


var checkType = require('../checkType');


/**
 * @class
 * @extends module:KwsMedia/elements~Session
 *
 * @param objectRef
 * @param {module:KwsMedia~MediaContainer} parent
 * @param {module:KwsMedia~MediaPipeline} pipeline
 * @param {module:KwsMedia/elements~Http.ConstructorParams} params
 */
function Http(objectRef, parent, pipeline, params)
{
  Session.call(this, objectRef, parent, pipeline);
};
Http.prototype.__proto__   = Session.prototype;
Http.prototype.constructor = Http;

/**
 * Get the {URL} where to download or upload the data
 *
 * @param {} callback
 *
 * @returns {external:String}
 */
Http.prototype.getUrl = function(callback)
{
  return this.invoke('getUrl', callback);
};


/**
 * 
 * @private
 * @param {module:KwsMedia/elements~Http.ConstructorParams} params
 *
 * @throws {SyntaxError}
 *
 * @returns {module:KwsMedia/elements~Http.ConstructorParams}
 */
Http.checkparams = function(params)
{
  /**
   * @type {module:KwsMedia/elements~Http.ConstructorParams}
   */
  var result = Session.checkparams(params);

  // check Http params
  for(var key in params)
  {
    var value = params[key];

    switch(key)
    {
      case 'disconnectionTimeout':
        checkType('Integer', key, value);
      break;

      default:
        continue;
    };

    result[key] = value;
  };
};


/**
 * @typedef module:KwsMedia/elements~Http.ConstructorParams
 *
 * @property {Boolean} excludeFromGC
 * @property {external:Number} garbageCollectorPeriod
 *
 * @property {Integer} disconnectionTimeout
 */


module.exports = Http;