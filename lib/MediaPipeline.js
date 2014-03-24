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
var async = require('async');
var MediaContainer = require('./MediaContainer');

var MediaElement = require('./MediaElement');
var MediaFilter  = require('./MediaFilter');
var MediaMixer   = require('./MediaMixer');

var endpoints = require('./endpoints');
var filters   = require('./filters');


var checkParams = require('./checkType').checkParams;


/**
 * Represent an instance of a server-side MediaObject
 *
 * @abstract
 * @class   module:KwsMedia~MediaPipeline
 * @extends module:KwsMedia~MediaContainer
 *
 * @param id
 * @param {module:KwsMedia~MediaContainer} parent
 */
function MediaPipeline(id, parent)
{
  var self = this;

  MediaContainer.call(this, id, parent);


 /**
   *
   *
   * @param {external:String} type - Type of the element
   * @param {external:string[]} [params]
   * @callback {createMediaObjectCallback} callback
   *
   * @return {module:KwsMedia~MediaPipeline} The pipeline itself
   */
  this.createMediaElement = function(type, params, callback)
  {
    // Fix optional parameters
    if(params instanceof Function)
    {
      if(callback)
        throw new SyntaxError("Nothing can be defined after the callback");

      callback = params;
      params = null;
    };

    callback = callback || function(){};

    /**
     * Request to the server to create a new MediaElement
     */
    function createMediaElement(item, callback)
    {
      // If element type is not registered, use generic MediaElement
      var constructor = endpoints[item.type] || filters[item.type] || MediaElement;

      item.constructorParams = checkParams(item.constructorParams, constructor);
      item.constructorParams.mediaPipeline = self.id;

      self._create(item, function(error, result)
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
   * @return {module:KwsMedia~MediaPipeline} The pipeline itself
   */
  this.createMediaMixer = function(type, params, callback)
  {
    // Fix optional parameters
    if(params instanceof Function)
    {
      if(callback)
        throw new SyntaxError("Nothing can be defined after the callback");

      callback = params;
      params = null;
    };

    callback = callback || function(){};

    var params2 =
    {
      type: type,
      mediaPipeline: self.id
    };

    if(params)
      params2.constructorParams = checkParams(params, MediaMixer);

    item.constructorParams.mediaPipeline = self.id;

    self._create(params2, function(error, result)
    {
      if(error) return callback(error);

      var id = result.value;

      var mediaMixer = new MediaMixer(id, self, self);

      // Exec successful callback if it's defined
      callback(null, mediaMixer);
    });

    return this;
  };


  /**
   * Connect the source of a media to the sink of the next one
   *
   * @param {...MediaContainer} media - A media to be connected
   * @callback {createMediaObjectCallback} [callback]
   *
   * @return {module:KwsMedia~MediaPipeline} The pipeline itself
   *
   * @throws {SyntaxError}
   */
  this.connect = function(media, callback)
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
      if(!(element instanceof MediaContainer || element instanceof MediaFilter))
        throw new SyntaxError("Only can connect elements of class 'MediaContainer' or 'MediaFilter'");
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
};
MediaPipeline.prototype.__proto__   = MediaContainer.prototype;
MediaPipeline.prototype.constructor = MediaPipeline;


/**
 * @see {@link module:KwsMedia~MediaContainer.paramsScheme}
 */
MediaPipeline.paramsScheme = MediaContainer.paramsScheme;


module.exports = MediaPipeline;
