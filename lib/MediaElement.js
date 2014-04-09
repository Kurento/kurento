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

var extend   = require('extend');
var inherits = require('inherits');

var checkType = require('./checkType');


/**
 * Media API for the Kurento Web SDK
 *
 * @module kwsMediaApi
 *
 * @copyright 2013-2014 Kurento (http://kurento.org/)
 * @license LGPL
 */

var MediaContainer = require('./MediaContainer');


/**
 * Basic building blocks of the media server, that can be interconnected through the API. A :rom:cls:`MediaElement` is a module that encapsulates a specific media capability. They can be connected to create media pipelines where those capabilities are applied, in sequence, to the stream going through the pipeline.

   :rom:cls:`MediaElement` objects are classified by its supported media type (audio, video, etc.) and the flow direction: :rom:cls:`MediaSource` pads are intended for media delivery while :rom:cls:`MediaSinks<MediaSink>`  behave as reception points.
 *
 * @abstract
 * @class   module:kwsMediaApi~MediaElement
 * @extends module:kwsMediaApi~MediaContainer
 */

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
 * @constructor
 *
 * @param {string} id
 * @param {module:kwsMediaApi~MediaContainer} parent
 * @param {module:kwsMediaApi~MediaPipeline} pipeline
 * @param {module:kwsMediaApi~MediaElement.constructorParams} params
 */
function MediaElement(id, parent, pipeline, params)
{
  MediaContainer.call(this, id, parent, pipeline, params);


  var sourcePads = undefined;
  var sinkPads   = undefined;


  /**
   * perform :rom:meth:`connect(sink,mediaType)` if there is exactly one sink for the given type, and their mediaDescriptions are the same
   *
   * @param {MediaElement} sink
   *  the target :rom:cls:`MediaElement`  from which :rom:cls:`MediaSink` will be obtained
   *
   * @param {MediaType} mediaType
   *  the :rom:enum:`MediaType` of the pads that will be connected
   *
   * @param {String} mediaDescription
   *  A textual description of the media source. Currently not used, aimed mainly for :rom:attr:`MediaType.DATA` sources
   *
   * @param {module:kwsMediaApi~MediaElement.connectCallback} [callback]
   *
   * @return {module:kwsMediaApi~MediaElement}
   *  The own media object
   */
  this.connect = function(sink, mediaType, mediaDescription, callback){
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

    checkType('MediaElement', 'sink', sink, {required: true});
    checkType('MediaType', 'mediaType', mediaType);
    checkType('String', 'mediaDescription', mediaDescription);

    // Generate request parameters
    var params =
    {
      sink: sink.id
    };

    if(mediaType != undefined)
    {
      params.type = mediaType;

      if(description)
        params.description = mediaDescription;
    };

    // Request to connect the elements
    this.invoke('connect', params, callback);

    return this;
  };
  /**
   * @callback MediaElement~connectCallback
   * @param {Error} error
   */

  /**
   * Get the :rom:cls:`sinks <MediaSink>` of this element
   *
   * @param {MediaPad.MediaType} [mediaType] - Type of the sink pads to be fetched
   * @param {external:String} [description] -
   * @param {module:kwsMediaApi~MediaElement.getMediaSinksCallback} [callback]
   *
   * @return {module:kwsMediaApi~MediaElement}
   *  The own media object
   */
  this.getMediaSinks = function(mediaType, description, callback){
    checkType('MediaType', 'mediaType', mediaType);
    checkType('String', 'description', description);

    if(sinkPads)
      callback(null, getPads(sinkPads, mediaType, description));

    else
    {
      var params = {};

      if(type != undefined)
      {
        params.type = mediaType;

        if(description != undefined)
          params.description = description;
      }

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
   * @callback MediaElement~getMediaSinksCallback
   * @param {Error} error
   * @param {MediaSink} result
   *  A list of sinks. The list will be empty if no sinks are found.
   */

  /**
   * Get the :rom:cls:`sources <MediaSource>` of this element
   *
   * @param {MediaPad.MediaType} [mediaType] - Type of the source pads to be fetched
   * @param {external:String} [description] -
   * @param {module:kwsMediaApi~MediaElement.getMediaSrcsCallback} [callback]
   *
   * @return {module:kwsMediaApi~MediaElement}
   *  The own media object
   */
  this.getMediaSrcs = function(mediaType, description, callback){
    checkType('MediaType', 'mediaType', mediaType);
    checkType('String', 'description', description);

    if(sourcePads)
      callback(null, getPads(sourcePads, mediaType, description));

    else
    {
      var params = {};

      if(mediaType != undefined)
      {
        params.type = mediaType;

        if(description != undefined)
          params.description = description;
      }
      else if(description != undefined)
        throw new SyntaxError("'type' is undefined while 'description' is not");

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
   * @callback MediaElement~getMediaSrcsCallback
   * @param {Error} error
   * @param {MediaSource} result
   *  A list of sources. The list will be empty if no sources are found.
   */
};
inherits(MediaElement, MediaContainer);


/**
 * @type   module:kwsMediaApi~MediaElement.constructorParams
 * @extend module:kwsMediaApi~MediaContainer.constructorParams
 */
MediaElement.constructorParams = {};
extend(MediaElement.constructorParams, MediaContainer.constructorParams);

/**
 * @type   module:kwsMediaApi~MediaElement.events
 * @extend module:kwsMediaApi~MediaContainer.events
 */
MediaElement.events = [];
MediaElement.events.concat(MediaContainer.events);


module.exports = MediaElement;
