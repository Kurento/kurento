/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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
 *
 *
 * @module
 *
 * @copyright 2013 Kurento (http://kurento.org/)
 * @license LGPL
 */

var MediaContainer = require('./MediaContainer');


/**
 *
 *
 * @class
 * @extends {MediaContainer}
 *
 * @param {string} type - Type of the mixer
 */
function MediaMixer(objectRef, parent, pipeline)
{
  MediaContainer.call(this, objectRef, parent, pipeline);


  /**
   * Type of the mixer
   *
   * @public
   * @readonly
   * @member {string}
   */
  Object.defineProperty(this, "type", {value : objectRef.type});


//  /**
//   *
//   *
//   * @param {Object} [params] -
//   * @callback {createMediaObjectCallback} [callback]
//   *
//   * @return {MediaMixer} The own mixer
//   */
//  this.createMixerEndPoint = function(params, callback)
//  {
//    if(params instanceof Function)
//    {
//      if(callback)
//        throw new SyntaxError;
//
//      callback = params;
//      params = null;
//    };
//
//    if(params)
//    {
//      params = MediaMixerEndPoint.checkParams(params);
//
//      params =
//      {
//        params: params
//      };
//    };
//
//    // Request to connect the elements
//    this._rpc('createMediaMixerEndPoint', params, function(error, result)
//    {
//      if(error)
//      {
//        if(callback)
//           callback(error);
//
//        return;
//      };
//
//      var mixerEndPoint = new MixerEndPoint(result, self);
//
//      // Exec successful callback if it's defined
//      if(callback)
//         callback(null, mixerEndPoint);
//    });
//
//    return this;
//  };
};
MediaMixer.prototype.__proto__   = MediaContainer.prototype;
MediaMixer.prototype.constructor = MediaMixer;


/**
 * 
 * @param {MediaObjectConstructorParams} params
 *
 * @throws {SyntaxError}
 *
 * @returns {MediaObjectConstructorParams}
 */
MediaMixer.checkparams = function(params)
{
  return MediaContainer.checkparams(params);
};


module.exports = MediaMixer;