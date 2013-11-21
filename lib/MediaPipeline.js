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

var MediaElement = require('./MediaElement');
var MediaMixer   = require('./MediaMixer');


/**
 *
 *
 * @class
 * @extends {MediaContainer}
 */
function MediaPipeline(objectRef, parent)
{
  var self = this;

  MediaContainer.call(this, objectRef, parent);


 /**
   *
   *
   * @param {string} type - Type of the element
   * @param {string[]} [params]
   * @callback {createMediaObjectCallback} callback
   *
   * @return {MediaPipeline} The pipeline itself
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

    var params2 =
    {
      type: type
    };

    if(params)
      params2.params = MediaElement.checkParams(params);

    this._rpc('createMediaElement', params2, function(error, objectRef)
    {
      if(error)
      {
        if(callback)
           callback(error);

        return;
      };

      var mediaElement = new MediaElement(objectRef, self, self);

      // Exec successful callback if it's defined
      if(callback)
         callback(null, mediaElement);
    });

    return this;
  };

  /**
   *
   *
   * @param {string} type - Type of the mixer
   * @param {string[]} [params]
   * @callback {createMediaObjectCallback} callback
   *
   * @return {MediaPipeline} The pipeline itself
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

    var params2 =
    {
      type: type
    };

    if(params)
      params2.params = MediaMixer.checkParams(params);

    this._rpc('createMediaMixer', params2, function(error, objectRef)
    {
      if(error)
      {
        if(callback)
           callback(error);

        return;
      };

      var mediaMixer = new MediaMixer(objectRef, self, self);

      // Exec successful callback if it's defined
      if(callback)
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
   * @return {MediaPipeline} The pipeline itself
   *
   * @throws {SyntaxError}
   */
  this.connect = function(media, callback)
  {
    // Fix lenght-variable arguments
    media = Array.prototype.slice.call(arguments, 0);
    callback = (media.length && typeof media[media.length - 1] == 'function')
             ? media.pop() : null;

    // Check if we have enought media components
    if(media.length < 2)
      throw new SyntaxError("Need at least two media elements to connect");

    function checkCorrectClass(element)
    {
      var klass = typeof sink;
      switch(klass)
      {
        case 'MediaElement':
        case 'MediaMixer':
          break;

        default:
          throw new SyntaxError("Can't connect elements of class '"+klass+"'");
      };
    };

    // Connect the media elements
    var src = media[0];
    checkCorrectClass(src);

    for(var i=1, sink; sink=media[i]; i++)
    {
      checkCorrectClass(sink);

      src.connect(sink);
      src = sink;
    };

    callback(null, this);

    return this;
  };
};
MediaPipeline.prototype.__proto__   = MediaContainer.prototype;
MediaPipeline.prototype.constructor = MediaPipeline;


/**
 * 
 * @param {MediaObjectConstructorParams} params
 *
 * @throws {SyntaxError}
 *
 * @returns {MediaObjectConstructorParams}
 */
MediaPipeline.checkparams = function(params)
{
  return MediaContainer.checkparams(params);
};


module.exports = MediaPipeline;