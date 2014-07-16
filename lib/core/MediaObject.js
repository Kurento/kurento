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


/**
 * Media API for the Kurento Web SDK
 *
 * @module kwsMediaApi/core
 *
 * @copyright 2013-2014 Kurento (http://kurento.org/)
 * @license LGPL
 */

var _MediaObject = require('../_MediaObject');


/**
 * Base for all objects that can be created in the media server.
 *
 * @abstract
 * @class   module:kwsMediaApi/core~MediaObject
 */

/**
 * @constructor
 *
 * @param {string} id
 */
function MediaObject(id)
{
  _MediaObject.call(this, id);
};
inherits(MediaObject, _MediaObject);


/**
 * :rom:cls:`MediaPipeline` to which this MediaObject belong, or the pipeline itself if invoked over a :rom:cls:`MediaPipeline`
 *
 * @param {module:kwsMediaApi/core~MediaObject.mediaPipelineCallback} [callback]
 *
 * @return {module:kwsMediaApi/core~MediaObject}
 *  The own media object
 */
MediaObject.prototype.getMediaPipeline = function(callback){
  return this.invoke('getMediaPipeline', callback);
};
/**
 * @callback MediaObject~getMediaPipelineCallback
 * @param {Error} error
 * @param {MediaPipeline} result
 */

/**
 * parent of this media object. The type of the parent depends on the type of the element. The parent of a :rom:cls:`MediaPad` is its :rom:cls:`MediaElement`; the parent of a :rom:cls:`Hub` or a :rom:cls:`MediaElement` is its :rom:cls:`MediaPipeline`. A :rom:cls:`MediaPipeline` has no parent, i.e. the property is null
 *
 * @param {module:kwsMediaApi/core~MediaObject.parentCallback} [callback]
 *
 * @return {module:kwsMediaApi/core~MediaObject}
 *  The own media object
 */
MediaObject.prototype.getParent = function(callback){
  return this.invoke('getParent', callback);
};
/**
 * @callback MediaObject~getParentCallback
 * @param {Error} error
 * @param {MediaObject} result
 */

/**
 * To test read/write property
 *
 * @param {module:kwsMediaApi/core~MediaObject.readWriteCallback} [callback]
 *
 * @return {module:kwsMediaApi/core~MediaObject}
 *  The own media object
 */
MediaObject.prototype.getReadWrite = function(callback){
  return this.invoke('getReadWrite', callback);
};
/**
 * @callback MediaObject~getReadWriteCallback
 * @param {Error} error
 * @param {boolean} result
 */

/**
 * To test readOnly property
 *
 * @param {module:kwsMediaApi/core~MediaObject.testReadOnlyCallback} [callback]
 *
 * @return {module:kwsMediaApi/core~MediaObject}
 *  The own media object
 */
MediaObject.prototype.getTestReadOnly = function(callback){
  return this.invoke('getTestReadOnly', callback);
};
/**
 * @callback MediaObject~getTestReadOnlyCallback
 * @param {Error} error
 * @param {String} result
 */


/**
 * @type module:kwsMediaApi/core~MediaObject.constructorParams
 */
MediaObject.constructorParams = {};

/**
 * @type module:kwsMediaApi/core~MediaObject.events
 */
MediaObject.events = ['Error'];


module.exports = MediaObject;


MediaObject.check = function(key, value)
{
  if(!(value instanceof MediaObject))
    throw SyntaxError(key+' param should be a MediaObject, not '+typeof value);
};
