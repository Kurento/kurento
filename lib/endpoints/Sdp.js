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
 * @param {module:KwsMedia/elements~Sdp.ConstructorParams} params
 */
function Sdp(objectRef, parent, pipeline, params)
{
  Session.call(this, objectRef, parent, pipeline);
};
Sdp.prototype.__proto__   = Session.prototype;
Sdp.prototype.constructor = Sdp;


Sdp.prototype.getLocalSdp = function(callback)
{
  return this.invoke('getLocalSdp', callback);
};

Sdp.prototype.getRemoteSdp = function(callback)
{
  return this.invoke('getRemoteSdp', callback);
};

Sdp.prototype.generateSdpOffer = function(callback)
{
  return this.invoke('generateSdpOffer', callback);
};

Sdp.prototype.processSdpOffer = function(offer, callback)
{
  checkType('String', 'offer', offer, true);

  return this.invoke('processSdpOffer', {offer: offer}, callback);
};

Sdp.prototype.processSdpAnswer = function(answer, callback)
{
  checkType('String', 'answer', answer, true);

  return this.invoke('processSdpAnswer', {answer: answer}, callback);
};


/**
 * 
 * @param {module:KwsMedia/elements~Sdp.ConstructorParams} params
 *
 * @throws {SyntaxError}
 *
 * @returns {module:KwsMedia/elements~Sdp.ConstructorParams}
 */
Sdp.checkparams = function(params)
{
  /**
   * @type {module:KwsMedia/elements~Sdp.ConstructorParams}
   */
  var result = Session.checkparams(params);

  // check HttpEndPoint params
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

  return result;
};


/**
 * @typedef module:KwsMedia/elements~Sdp.ConstructorParams
 * 
 * @property {Boolean} excludeFromGC
 * @property {external:Number} garbageCollectorPeriod
 *
 * @property {Integer} disconnectionTimeout
 */


module.exports = Sdp;