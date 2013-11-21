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
 * @module KwsMedia
 *
 * @copyright 2013 Kurento (http://kurento.org/)
 * @license LGPL
 */

var MediaObject = require('./MediaObject');


/**
 * Represent an instance of a MediaContainer
 *
 * This is a Javascript API abstraction not available on the Media Server API
 *
 * @abstract
 * @class
 * @extends {module:KwsMedia~MediaObject}
 *
 * @param {} objectRef
 * @param {*} parent
 * @param {module:KwsMedia~MediaPipeline} [pipeline]
 */
function MediaContainer(objectRef, parent, pipeline, params)
{
  MediaObject.call(this, objectRef, parent, pipeline, params);


  // Bubble the event for new MediaObjects
  this.on('mediaObject', function(mediaObject)
  {
    parent.emit('mediaObject', mediaObject);
  });
};
MediaContainer.prototype.__proto__   = MediaObject.prototype;
MediaContainer.prototype.constructor = MediaContainer;


/**
 * 
 * @param {MediaObjectConstructorParams} params
 *
 * @throws {SyntaxError}
 *
 * @returns {MediaObjectConstructorParams}
 */
MediaContainer.checkparams = function(params)
{
  return MediaObject.checkparams(params);
};


module.exports = MediaContainer;