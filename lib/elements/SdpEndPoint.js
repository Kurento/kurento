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
 * @param {MediaHttpEndPointConstructorParams} params
 */
function SdpEndPoint(objectRef, parent, pipeline, params)
{
  SessionEndPoint.call(this, objectRef, parent, pipeline);
};
SdpEndPoint.prototype.__proto__   = SessionEndPoint.prototype;
SdpEndPoint.prototype.constructor = SdpEndPoint;


SdpEndPoint.prototype.getLocalSdp = function(callback)
{
  return this.invoke('getLocalSdp', callback);
};

SdpEndPoint.prototype.getRemoteSdp = function(callback)
{
  return this.invoke('getRemoteSdp', callback);
};

SdpEndPoint.prototype.generateSdpOffer = function(callback)
{
  return this.invoke('generateSdpOffer', callback);
};

SdpEndPoint.prototype.processSdpOffer = function(offer, callback)
{
  return this.invoke('processSdpOffer', {offer: offer}, callback);
};

SdpEndPoint.prototype.processSdpAnswer = function(answer, callback)
{
  return this.invoke('processSdpAnswer', {answer: answer}, callback);
};


/**
 * 
 * @param {module:KwsMedia/elements~SdpEndPoint.ConstructorParams} params
 *
 * @throws {SyntaxError}
 *
 * @returns {module:KwsMedia/elements~SdpEndPoint.ConstructorParams}
 */
SdpEndPoint.checkparams = function(params)
{
  /**
   * @type {module:KwsMedia/elements~SdpEndPoint.ConstructorParams}
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
 * @typedef module:KwsMedia/elements~SdpEndPoint.ConstructorParams
 * 
 * @property {Boolean} excludeFromGC
 * @property {external:Number} garbageCollectorPeriod
 *
 * @property {Integer} disconnectionTimeout
 */


module.exports = SdpEndPoint;