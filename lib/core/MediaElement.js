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
 * Basic building blocks of the media server, that can be interconnected through the API. A :rom:cls:`MediaElement` is a module that encapsulates a specific media capability. They can be connected to create media pipelines where those capabilities are applied, in sequence, to the stream going through the pipeline.

   :rom:cls:`MediaElement` objects are classified by its supported media type (audio, video, etc.) and the flow direction: :rom:cls:`MediaSource` pads are intended for media delivery while :rom:cls:`MediaSinks<MediaSink>`  behave as reception points.
 *
 * @abstract
 * @class   module:kwsMediaApi/core~MediaElement
 * @extends module:kwsMediaApi~MediaObject
 */

/**
 * @constructor
 *
 * @param {string} id
 */
function MediaElement(id)
{
  MediaObject.call(this, id);
};
inherits(MediaElement, MediaObject);


/**
 * perform :rom:meth:`connect(sink,mediaType)` if there is exactly one sink for the given type, and their mediaDescriptions are the same
 *
 * @param {MediaElement} sink
 *  the target :rom:cls:`MediaElement`  from which :rom:cls:`MediaSink` will be obtained
 *
 * @param {MediaPad.MediaType} [mediaType]
 *  the :rom:enum:`MediaType` of the pads that will be connected
 *
 * @param {external:String} [mediaDescription]
 *  A textual description of the media source. Currently not used, aimed mainly for :rom:attr:`MediaType.DATA` sources
 *
 * @param {module:kwsMediaApi/core~MediaElement.connectCallback} [callback]
 *
 * @return {module:kwsMediaApi/core~MediaElement}
 *  The own media object
 */
MediaElement.prototype.connect = function(sink, mediaType, mediaDescription, callback){
  // Fix optional parameters
  if(mediaType instanceof Function)
  {
    if(mediaDescription)
      throw new SyntaxError("Nothing can be defined after the callback");

    callback = mediaType;
    mediaDescription = undefined;
    mediaType = undefined;
  }

  else if(mediaDescription instanceof Function)
  {
    if(callback)
      throw new SyntaxError("Nothing can be defined after the callback");

    callback = mediaDescription;
    mediaDescription = undefined;
  };

  if(!mediaType && mediaDescription)
    throw new SyntaxError("'mediaType' is undefined while 'mediaDescription' is not");

  checkType('MediaElement', 'sink', sink, {required: true});
  checkType('MediaType', 'mediaType', mediaType);
  checkType('String', 'mediaDescription', mediaDescription);

  var params = {
    sink: sink.id,
    mediaType: mediaType,
    mediaDescription: mediaDescription,
  };

  this.invoke('connect', params, callback);

  return this;
};
/**
 * @callback MediaElement~connectCallback
 * @param {Error} error
 */

/**
 * A list of sinks of the given :rom:ref:`MediaType`. The list will be empty if no sinks are found.
 *
 * @param {MediaPad.MediaType} [mediaType]
 *  One of :rom:attr:`MediaType.AUDIO`, :rom:attr:`MediaType.VIDEO` or :rom:attr:`MediaType.DATA`
 *
 * @param {external:String} [description]
 *  A textual description of the media source. Currently not used, aimed mainly for :rom:attr:`MediaType.DATA` sources
 *
 * @param {module:kwsMediaApi/core~MediaElement.getMediaSinksCallback} [callback]
 *
 * @return {module:kwsMediaApi/core~MediaElement}
 *  The own media object
 */
MediaElement.prototype.getMediaSinks = function(mediaType, description, callback){
  // Fix optional parameters
  if(mediaType instanceof Function)
  {
    if(description)
      throw new SyntaxError("Nothing can be defined after the callback");

    callback = mediaType;
    description = undefined;
    mediaType = undefined;
  }

  else if(description instanceof Function)
  {
    if(callback)
      throw new SyntaxError("Nothing can be defined after the callback");

    callback = description;
    mediaDescription = undefined;
  };

  if(!mediaType && description)
    throw new SyntaxError("'mediaType' is undefined while 'description' is not");

  checkType('MediaType', 'mediaType', mediaType);
  checkType('String', 'description', description);

  var params = {
    mediaType: mediaType,
    description: description,
  };

  this.invoke('getMediaSinks', params, callback);

  return this;
};
/**
 * @callback MediaElement~getMediaSinksCallback
 * @param {Error} error
 * @param {MediaSink} result
 *  A list of sinks. The list will be empty if no sinks are found.
 */

/**
 * Get the media sources of the given type and description
 *
 * @param {MediaPad.MediaType} [mediaType]
 *  One of :rom:attr:`MediaType.AUDIO`, :rom:attr:`MediaType.VIDEO` or :rom:attr:`MediaType.DATA`
 *
 * @param {external:string} [description]
 *  A textual description of the media source. Currently not used, aimed mainly for :rom:attr:`MediaType.DATA` sources
 *
 * @param {module:kwsMediaApi/core~MediaElement.getMediaSrcsCallback} [callback]
 *
 * @return {module:kwsMediaApi/core~MediaElement}
 *  The own media object
 */
MediaElement.prototype.getMediaSrcs = function(mediaType, description, callback){
  // Fix optional parameters
  if(mediaType instanceof Function)
  {
    if(description)
      throw new SyntaxError("Nothing can be defined after the callback");

    callback = mediaType;
    description = undefined;
    mediaType = undefined;
  }

  else if(description instanceof Function)
  {
    if(callback)
      throw new SyntaxError("Nothing can be defined after the callback");

    callback = description;
    mediaDescription = undefined;
  };

  if(!mediaType && description)
    throw new SyntaxError("'mediaType' is undefined while 'description' is not");

  checkType('MediaType', 'mediaType', mediaType);
  checkType('String', 'description', description);

  var params = {
    mediaType: mediaType,
    description: description,
  };

  this.invoke('getMediaSrcs', params, callback);

  return this;
};
/**
 * @callback MediaElement~getMediaSrcsCallback
 * @param {Error} error
 * @param {MediaSource} result
 *  A list of sources. The list will be empty if no sources are found.
 */


/**
 * @type module:kwsMediaApi/core~MediaElement.constructorParams
 */
MediaElement.constructorParams = {};

/**
 * @type   module:kwsMediaApi/core~MediaElement.events
 * @extend module:kwsMediaApi~MediaObject.events
 */
MediaElement.events = [];
MediaElement.events.concat(MediaObject.events);


module.exports = MediaElement;


MediaElement.check = function(key, value)
{
  if(!(value instanceof MediaElement))
    throw SyntaxError(key+' param should be a MediaElement, not '+typeof value);
};
