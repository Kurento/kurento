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

var MediaContainer = require('./MediaContainer');


var checkParams = require('./checkType').checkParams;


/**
 * Represent an instance of a MediaFilter
 *
 * @abstract
 * @class   module:KwsMedia~MediaMixer
 * @extends module:KwsMedia~MediaContainer
 *
 * @param id
 * @param {module:KwsMedia~MediaContainer} parent
 * @param {module:KwsMedia~MediaPipeline} [pipeline]
 */
function MediaMixer(id, parent, pipeline)
{
  MediaContainer.call(this, id, parent, pipeline);


//  /**
//   * Type of the mixer
//   *
//   * @public
//   * @readonly
//   * @member {external:String}
//   */
//  Object.defineProperty(this, "type", {value : objectRef.type});


//  /**
//   *
//   *
//   * @param {Object} [params] -
//   * @callback {createMediaObjectCallback} [callback]
//   *
//   * @return {MediaMixer} The own mixer
//   */
//  this.createMixerEndpoint = function(params, callback)
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
//    callback = callback || function(){};
//
//    var params2 =
//    {
//      type: 'MixerEndpoint'
//    };
//
//    if(params)
//      params2.constructorParams = checkParams(params, MediaMixerEndpoint);
//
//    // Request to connect the elements
//    self._create(params2, function(error, result)
//    {
//      if(error) return callback(error);
//
//      var id = result.value;
//
//      var mixerEndpoint = new MixerEndpoint(id, self);
//
//      // Exec successful callback if it's defined
//      callback(null, mixerEndpoint);
//    });
//
//    return this;
//  };
};
MediaMixer.prototype.__proto__   = MediaContainer.prototype;
MediaMixer.prototype.constructor = MediaMixer;


/**
 * @see {@link module:KwsMedia~MediaContainer.paramsScheme}
 */
MediaMixer.paramsScheme = MediaContainer.paramsScheme;


module.exports = MediaMixer;
