/*
 * (C) Copyright 2013-2014 Kurento (http://kurento.org/)
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

var inherits = require('inherits');

var checkType = require('../checkType');


/**
 * Media API for the Kurento Web SDK
 *
 * @module kwsMediaApi/core
 *
 * @copyright 2013-2014 Kurento (http://kurento.org/)
 * @license LGPL
 */

var MediaObject = require('./MediaObject');


/**
 * A :rom:cls:`MediaPad` is an element´s interface with the outside world. The data streams flow from the :rom:cls:`MediaSource` pad to another element's :rom:cls:`MediaSink` pad.
 *
 * @abstract
 * @class   module:kwsMediaApi/core~MediaPad
 * @extends module:kwsMediaApi~MediaObject
 */

/**
 * @constructor
 *
 * @param {string} id
 */
function MediaPad(id)
{
  MediaObject.call(this, id);
};
inherits(MediaPad, MediaObject);


/**
 * Obtains the description for this pad.

   This method does not make a request to the media server, and is included to keep the simmetry with the rest of methods from the API.
 *
 * @param {module:kwsMediaApi/core~MediaPad.getMediaDescriptionCallback} [callback]
 *
 * @return {module:kwsMediaApi/core~MediaPad}
 *  The own media object
 */
MediaPad.prototype.getMediaDescription = function(callback){
  return this.invoke('getMediaDescription', callback);
};
/**
 * @callback MediaPad~getMediaDescriptionCallback
 * @param {Error} error
 * @param {String} result
 *  The description
 */

/**
 * Obtains the :rom:cls:`MediaElement` that encloses this pad
 *
 * @param {module:kwsMediaApi/core~MediaPad.getMediaElementCallback} [callback]
 *
 * @return {module:kwsMediaApi/core~MediaPad}
 *  The own media object
 */
MediaPad.prototype.getMediaElement = function(callback){
  return this.invoke('getMediaElement', callback);
};
/**
 * @callback MediaPad~getMediaElementCallback
 * @param {Error} error
 * @param {MediaElement} result
 *  the element
 */

/**
 * Obtains the type of media that this pad accepts
 *
 * @param {module:kwsMediaApi/core~MediaPad.getMediaTypeCallback} [callback]
 *
 * @return {module:kwsMediaApi/core~MediaPad}
 *  The own media object
 */
MediaPad.prototype.getMediaType = function(callback){
  return this.invoke('getMediaType', callback);
};
/**
 * @callback MediaPad~getMediaTypeCallback
 * @param {Error} error
 * @param {MediaType} result
 *  One of :rom:attr:`MediaType.AUDIO`, :rom:attr:`MediaType.DATA` or :rom:attr:`MediaType.VIDEO`
 */


/**
 * @type module:kwsMediaApi/core~MediaPad.constructorParams
 */
MediaPad.constructorParams = {};

/**
 * @type   module:kwsMediaApi/core~MediaPad.events
 * @extend module:kwsMediaApi~MediaObject.events
 */
MediaPad.events = [];
MediaPad.events.concat(MediaObject.events);


module.exports = MediaPad;


MediaPad.check = function(key, value)
{
  if(!(value instanceof MediaPad))
    throw SyntaxError(key+' param should be a MediaPad, not '+typeof value);
};
