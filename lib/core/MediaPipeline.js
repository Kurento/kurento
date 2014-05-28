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

var Promise = require('es6-promise').Promise;


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
 * A pipeline is a container for a collection of :rom:cls:`MediaElements<MediaElement>` and :rom:cls:`MediaMixers<MediaMixer>`. It offers the methods needed to control the creation and connection of elements inside a certain pipeline.
 *
 * @class   module:kwsMediaApi/core~MediaPipeline
 * @extends module:kwsMediaApi~MediaObject
 */

/**
 * Create a :rom:cls:`MediaPipeline`
 *
 * @constructor
 *
 * @param {string} id
 */
function MediaPipeline(id)
{
  MediaObject.call(this, id);
};
inherits(MediaPipeline, MediaObject);


/**
 * Create a new instance of a {module:kwsMediaApi/core~MediaObject} attached to this {module:kwsMediaApi/core~MediaPipeline}
 *
 * @param {external:string} type - Type of the {module:kwsMediaApi/core~MediaObject}
 * @param {external:string[]} [params]
 * @callback {module:kwsMediaApi/core~MediaPipeline~createCallback} callback
 *
 * @return {module:kwsMediaApi/core~MediaPipeline} The pipeline itself
 */
MediaPipeline.prototype.create = function(type, params, callback){
  var self = this;

  // Fix optional parameters
  if(params instanceof Function){
    if(callback)
      throw new SyntaxError("Nothing can be defined after the callback");

    callback = params;
    params = undefined;
  };

  params = params || {};

  var promise = new Promise(function(resolve, reject)
  {
    params.mediaPipeline = self;

    self.emit('_create', type, params, function(error, result)
    {
      if(error) return reject(error);

      resolve(result);
    });
  });

  if(callback)
    promise.then(function(result)
    {
      callback(null, result);
    },
    callback);

  return promise;
};
/**
 * @callback module:kwsMediaApi/core~MediaPipeline~createCallback
 * @param {Error} error
 * @param {module:kwsMediaApi/core~MediaElement} result
 *  The created MediaElement
 */


/**
 * @type module:kwsMediaApi/core~MediaPipeline.constructorParams
 */
MediaPipeline.constructorParams = {};

/**
 * @type   module:kwsMediaApi/core~MediaPipeline.events
 * @extend module:kwsMediaApi~MediaObject.events
 */
MediaPipeline.events = [];
MediaPipeline.events.concat(MediaObject.events);


module.exports = MediaPipeline;


MediaPipeline.check = function(key, value)
{
  if(!(value instanceof MediaPipeline))
    throw SyntaxError(key+' param should be a MediaPipeline, not '+typeof value);
};
