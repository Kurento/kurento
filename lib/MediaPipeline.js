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

var async    = require('async');
var extend   = require('extend');
var inherits = require('inherits');


/**
 * Media API for the Kurento Web SDK
 *
 * @module kwsMediaApi
 *
 * @copyright 2013-2014 Kurento (http://kurento.org/)
 * @license LGPL
 */

var MediaContainer = require('./MediaContainer');

var MediaElement = require('./MediaElement');
var Filter       = require('./Filter');
var Hub          = require('./Hub');

var endpoints = require('./endpoints');
var filters   = require('./filters');


var checkParams = require('./checkType').checkParams;


/**
 * A pipeline is a container for a collection of :rom:cls:`MediaElements<MediaElement>` and :rom:cls:`MediaMixers<MediaMixer>`. It offers the methods needed to control the creation and connection of elements inside a certain pipeline.
 *
 * @class   module:kwsMediaApi~MediaPipeline
 * @extends module:kwsMediaApi~MediaContainer
 */

/**
 * Create a :rom:cls:`MediaPipeline`
 *
 * @constructor
 *
 * @param {string} id
 * @param {module:kwsMediaApi~MediaContainer} parent
 */
function MediaPipeline(id, parent)
{
  MediaContainer.call(this, id, parent);
};
inherits(MediaPipeline, MediaContainer);


/**
 * Connect the source of a media to the sink of the next one
 *
 * @param {...MediaContainer} media - A media to be connected
 * @callback {createMediaObjectCallback} [callback]
 *
 * @return {module:kwsMediaApi~MediaPipeline} The pipeline itself
 *
 * @throws {SyntaxError}
 */
MediaPipeline.prototype.connect = function(media, callback)
{
  // Fix lenght-variable arguments
  media = Array.prototype.slice.call(arguments, 0);
  callback = (media.length && typeof media[media.length - 1] == 'function')
           ? media.pop() : function(){};

  // Check if we have enought media components
  if(media.length < 2)
    throw new SyntaxError("Need at least two media elements to connect");

  function checkCorrectClass(element)
  {
    if(!(element instanceof MediaElement || element instanceof Hub))
      throw new SyntaxError("Only can connect elements of class 'MediaElement' or 'Hub'");
  };

  // Connect the media elements
  var src = media[0];
  checkCorrectClass(src);

  for(var i=1, sink; sink=media[i]; i++)
  {
    checkCorrectClass(sink);

    src.connect(sink, function(){});
    src = sink;
  };

  callback(null, this);

  return this;
};


/**
 *
 *
 * @param {external:String} type - Type of the element
 * @param {external:string[]} [params]
 * @callback {createMediaObjectCallback} callback
 *
 * @return {module:kwsMediaApi~MediaPipeline} The pipeline itself
 */
MediaPipeline.prototype.createMediaElement = function(type, params, callback)
{
  var self = this;

  // Fix optional parameters
  if(params instanceof Function)
  {
    if(callback)
      throw new SyntaxError("Nothing can be defined after the callback");

    callback = params;
    params = undefined;
  };

  callback = callback || function(){};

  /**
   * Request to the server to create a new MediaElement
   */
  function createMediaElement(item, callback)
  {
    // If element type is not registered, use generic MediaElement
    var constructor = endpoints[item.type] || filters[item.type] || MediaElement;

    item.constructorParams.mediaPipeline = self.id;
    item.constructorParams = checkParams(item.constructorParams,
        constructor.constructorParams, constructor.name);

    self.emit('_rpc', 'create', item, function(error, result)
    {
      if(error) return callback(error);

      var id = result.value;

      var params = item.params || {};

      var mediaElement = new constructor(id, self, self, params);

      // Exec successful callback if it's defined
      callback(null, mediaElement);
    });
  };

  function createItem(params, type)
  {
    var item =
    {
      constructorParams: params || {},
      type: type
    };

    return item;
  }

  if(type instanceof Array)
  {
    var items = [];
    for(var i=0, t; t=type[i]; i++)
      items[i] = createItem(params[i], t);

    async.map(items, createMediaElement, callback);
  }
  else
    createMediaElement(createItem(params, type), callback);

  return this;
};

/**
 *
 *
 * @param {external:String} type - Type of the mixer
 * @param {external:string[]} [params]
 * @callback {createMediaObjectCallback} callback
 *
 * @return {module:kwsMediaApi~MediaPipeline} The pipeline itself
 */
MediaPipeline.prototype.createHub = function(type, params, callback)
{
  var self = this;

  // Fix optional parameters
  if(params instanceof Function)
  {
    if(callback)
      throw new SyntaxError("Nothing can be defined after the callback");

    callback = params;
    params = undefined;
  };

  callback = callback || function(){};

  var params2 =
  {
    type: type,
    mediaPipeline: self.id
  };

  if(params)
    params2.constructorParams = checkParams(params, Hub.constructorParams, 'Hub');

  item.constructorParams.mediaPipeline = self.id;

  this.emit('_rpc', 'create', params2, function(error, result)
  {
    if(error) return callback(error);

    var id = result.value;

    var hub = new Hub(id, self, self);

    // Exec successful callback if it's defined
    callback(null, hub);
  });

  return this;
};


/**
 * @type   module:kwsMediaApi~MediaPipeline.constructorParams
 * @extend module:kwsMediaApi~MediaContainer.constructorParams
 */
MediaPipeline.constructorParams = {};
extend(MediaPipeline.constructorParams, MediaContainer.constructorParams);

/**
 * @type   module:kwsMediaApi~MediaPipeline.events
 * @extend module:kwsMediaApi~MediaContainer.events
 */
MediaPipeline.events = [];
MediaPipeline.events.concat(MediaContainer.events);


module.exports = MediaPipeline;
