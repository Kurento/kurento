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
 * @module kwsMediaApi
 *
 * @copyright 2013 Kurento (http://kurento.org/)
 * @license LGPL
 */
var MediaContainer = require('./MediaContainer');


/**
 *
 *
 * @private
 * @class   module:kwsMediaApi~MediaElement
 * @extends module:kwsMediaApi~MediaContainer
 *
 * @param id
 * @param parent
 * @param pipeline
 * @param {module:kwsMediaApi~MediaObject.ConstructorParams} params
 */
function MediaElement(id, parent, pipeline, params)
{
  MediaContainer.call(this, id, parent, pipeline, params);


  var sourcePads = undefined;
  var sinkPads   = undefined;


//  /**
//   * Type of the element
//   *
//   * @public
//   * @readonly
//   * @member {external:String}
//   */
//  Object.defineProperty(this, "type", {value : objectRef.type});


  function checkMediaType(type)
  {
    switch(type)
    {
      case "audio":
      case "video":
      case "data":
        break;

      default:
        throw new SyntaxError(type+" is not of the valid ones");
    };
  };


  function getPads(pads, type, description)
  {
    var result = [];

    for(var i=0, pad; pad=pads[i]; i++)
    {
      if(type && type != pad.type)
        continue;

      if(description && description != pad.description)
        continue;

      result.append(pad);
    };

    return result;
  };


  /**
   *
   *
   * @param {MediaPad.MediaType} [type] - Type of the source pads to be fetched
   * @param {external:String} [description] -
   * @callback {createMediaObjectCallback} [callback]
   *
   * @return {MediaElement} The element itself
   *
   * @throws {MediaServerException}
   * @throws {SyntaxError} Type is not of the valid ones
   */
  this.getMediaSrcs = function(type, description, callback)
  {
    checkMediaType(type);

    if(sourcePads)
      callback(null, getPads(sourcePads, type, description));

    else
    {
      var params = {};

      if(type)
        params.type = type;

      if(description)
        params.description = description;

      this.invoke('getMediaSrcs', params, function(error, pads)
      {
        if(error) return callback(error);

        sourcePads = pads;

        callback(null, getPads(sourcePads, type, description));
      });
    };

    return this;
  };

  /**
   *
   *
   * @param {MediaPad.MediaType} [type] - Type of the sink pads to be fetched
   * @param {external:String} [description] -
   * @callback {createMediaObjectCallback} [callback]
   *
   * @return {MediaElement} The element itself
   *
   * @throws {MediaServerException}
   *
   * @todo Add support for description string
   */
  this.getMediaSinks = function(type, description, callback)
  {
    checkMediaType(type);

    if(sinkPads)
      callback(null, getPads(sinkPads, type, description));

    else
    {
      var params = {};

      if(type)
        params.type = type;

      if(description)
        params.description = description;

      this.invoke('getMediaSinks', params, function(error, pads)
      {
        if(error) return callback(error);

        sinkPads = pads;

        callback(null, getPads(sinkPads, type, description));
      });
    };

    return this;
  };


  /**
   *
   *
   * @param {module:kwsMediaApi~MediaElement}        sink
   *  Element to connect
   *
   * @param {module:kwsMediaApi~MediaPad.MediaType} [type]
   *  Type of the sink pads to be fetched
   *
   * @param {external:String}                    [description]
   *
   *
   * @return {module:kwsMediaApi~MediaElement}
   *  The element itself
   *
   * @throws {module:kwsMediaApi~MediaServerException}
   */
  this.connect = function(sink, type, description, callback)
  {
    // Fix optional parameters
    if(type instanceof Function)
    {
      if(description)
        throw new SyntaxError("Nothing can be defined after the callback");

      description = type;
      type = null;
    };

    if(description instanceof Function)
    {
      if(callback)
        throw new SyntaxError("Nothing can be defined after the callback");

      callback = description;
      description = null;
    };

    // Generate request parameters
    var params =
    {
      sink: sink.id
    };

    if(type)
    {
      params.type = type;

      if(description)
        params.description = description;
    };

    // Request to connect the elements
    this.invoke('connect', params, callback);

    return this;
  };
}
MediaElement.prototype.__proto__   = MediaContainer.prototype;
MediaElement.prototype.constructor = MediaElement;


/**
 * @see {@link module:kwsMediaApi~MediaObject.constructorParams}
 */
MediaElement.constructorParams = MediaContainer.constructorParams;


module.exports = MediaElement;
