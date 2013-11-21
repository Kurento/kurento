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

var MediaElement = require('../MediaElement');


/**
 * @class
 * @extends module:KwsMedia/elements~UriEndPoint
 *
 * @param objectRef
 * @param parent
 * @param pipeline
 * @param {module:KwsMedia/elements~UriEndPoint.ConstructorParams} params
 */
function UriEndPoint(objectRef, parent, pipeline, params)
{
  MediaElement.call(this, objectRef, parent, pipeline, params);
};
UriEndPoint.prototype.__proto__   = MediaElement.prototype;
UriEndPoint.prototype.constructor = UriEndPoint;


UriEndPoint.prototype.getUri = function(callback)
{
  return this.invoke('getUri', callback);
};

UriEndPoint.prototype.start = function(callback)
{
  return this.invoke('start', callback);
};

UriEndPoint.prototype.pause = function(callback)
{
  return this.invoke('pause', callback);
};

UriEndPoint.prototype.stop = function(callback)
{
  return this.invoke('stop', callback);
};


/**
 * 
 * @param {module:KwsMedia/elements~UriEndPoint.ConstructorParams} params
 *
 * @throws {SyntaxError}
 *
 * @returns {module:KwsMedia/elements~UriEndPoint.ConstructorParams}
 */
UriEndPoint.checkparams = function(params)
{
  /**
   * @type {module:KwsMedia/elements~UriEndPoint.ConstructorParams}
   */
  var result = MediaElement.checkparams(params);

  // check MediaObject params
  for(var key in params)
  {
    var value = params[key];

    switch(key)
    {
      case 'uri':
        if(typeof value != 'string')
          throw SyntaxError(key+" param should be String");
      break;

      default:
        continue;
    };

    result[key] = value;
  };

  return result;
};


/**
 * @typedef module:KwsMedia/elements~UriEndPoint.ConstructorParams
 * 
 * @property {Boolean} excludeFromGC
 * @property {external:Number} garbageCollectorPeriod
 *
 * @property {external:String} uri
 */


module.exports = UriEndPoint;