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

var MediaObject = require('./MediaObject');


/**
 * A :rom:cls:`MediaPad` is an element´s interface with the outside world. The data streams flow from the :rom:cls:`MediaSource` pad to another element's :rom:cls:`MediaSink` pad.
 *
 * @abstract
 * @class   module:kwsMediaApi~MediaPad
 * @extends module:kwsMediaApi~MediaObject
 */

/**
 *
 * @constructor
 *
 * @param {string} id
 * @param {module:kwsMediaApi~MediaContainer} parent
 * @param {module:kwsMediaApi~MediaPipeline} pipeline
 * @param {module:kwsMediaApi~MediaPad.constructorParams} params
 */
function MediaPad(id, parent, pipeline, mediaElement, description)
{
  MediaObject.call(this, id, parent, pipeline);

  /**
   * {@link module:kwsMediaApi~MediaElement} that owns this {@link module:kwsMediaApi~MediaPad}
   *
   * @public
   * @readonly
   * @member {module:kwsMediaApi~MediaElement} mediaElement
   * @memberof module:kwsMediaApi~MediaPad
   * @instance
   */
  Object.defineProperty(this, "mediaElement", {value : mediaElement});

//  /**
//   * Type of this {@link module:kwsMediaApi~MediaPad}
//   *
//   * @public
//   * @readonly
//   * @member {module:kwsMediaApi~MediaPad.MediaType} type
//   * @memberof module:kwsMediaApi~MediaPad
//   * @instance
//   */
//  Object.defineProperty(this, "type", {value : objectRef.type});

  /**
   * Description string of this {MediaPad}
   *
   * @public
   * @readonly
   * @member {external:String} description
   * @memberof module:kwsMediaApi~MediaPad
   * @instance
   */
  Object.defineProperty(this, "description", {value : description});
};
inherits(MediaPad, MediaObject);


/**
 *
 *
 * @private
 * @class
 * @extends {module:kwsMediaApi~MediaPad}
 *
 * @param {module:kwsMediaApi~MediaElement} mediaElement - Element owner of this pad
 * @param {module:kwsMediaApi~MediaPad.MediaType} type - Type of this pad
 * @param {external:String} description - Description of this pad
 */
function MediaPadSource(id, parent, pipeline, mediaElement, description)
{
  MediaPad.call(this, id, parent, pipeline, mediaElement, description);


  var sinks = [];


  /**
   * Connect this source pad to the specified sink pad
   *
   * @public
   *
   * @param {...MediaPadSink} sink - Sink pad to be connected
   *
   * @return {MediaPadSource} The own pad
   *
   * @throws {MediaServerError}
   */
  this.connect = function(sink, callback)
  {
    var params =
    {
      sink: sink.id
    };

    callback = callback || function(){};

    this.invoke('connect', params, function(error)
    {
      if(error) return callback(error);

      Object.defineProperty(sink, 'source', {value: this});
    });

    return this;
  };

  /**
   * Disconnect this source pad from the specified sink pad
   *
   * @public
   *
   * @param {...MediaPadSink} sink - Sink pad to be disconnected
   *
   * @return {MediaPadSource} The own pad
   *
   * @throws {MediaServerError}
   */
  this.disconnect = function(sink, callback)
  {
    var params =
    {
      sink: sink.id
    };

    callback = callback || function(){};

    this.invoke('disconnect', params, function(error)
    {
      if(error) return callback(error);

      Object.defineProperty(sink, 'source', {value: undefined});
    });

    return this;
  };

  /**
   * List of sink pads currently connected to this source pad
   *
   * @public
   * @readonly
   * @member {module:kwsMediaApi~MediaPadSink[]} sinks
   * @memberof module:kwsMediaApi~MediaPadSource
   * @instance
   *
   * @throws {module:kwsMediaApi~MediaServerException}
   */
  this.__defineGetter__('sinks', function()
  {
    return sinks;
  });
}
inherits(MediaPadSource, MediaPad);


/**
 *
 *
 * @private
 * @class
 * @extends {module:kwsMediaApi~MediaPad}
 *
 * @param {MediaElement} mediaElement - Element owner of this pad
 * @param {MediaPad.MediaType} type - Type of this pad
 * @param {external:String} description - Description of this pad
 */
function MediaPadSink(id, parent, pipeline, mediaElement, description)
{
  MediaPad.call(this, id, parent, pipeline, mediaElement, description);


  /**
   * Source pad currently connected to this sink pad
   *
   * @public
   * @readonly
   * @member {MediaPadSource} source
   * @memberof module:kwsMediaApi~MediaPadSink
   * @instance
   */
  Object.defineProperty(this, 'source', {enumerable: true});
}
inherits(MediaPadSink, MediaPad);


/**
 * Disconnect this sink pad from its source pad
 *
 * @public
 *
 * @param {...MediaPadSource} source - Source pad to be disconnected
 *
 * @return {MediaPadSink} The own pad
 *
 * @throws {MediaServerError}
 */
MediaPadSink.prototype.disconnect = function(source, callback)
{
  var params =
  {
    source: source
  };

  callback = callback || function(){};

  this.invoke('disconnect', params, function(error)
  {
    if(error) return callback(error);

    Object.defineProperty(this, 'source', {value: undefined});
  });

  return this;
};

/**
 * @type   module:kwsMediaApi~MediaPad.constructorParams
 * @extend module:kwsMediaApi~MediaObject.constructorParams
 */
MediaPad.constructorParams = {};
extend(MediaPad.constructorParams, MediaObject.constructorParams);

/**
 * @type   module:kwsMediaApi~MediaPad.events
 * @extend module:kwsMediaApi~MediaObject.events
 */
MediaPad.events = [];
MediaPad.events.concat(MediaObject.events);


exports.MediaPadSink   = MediaPadSink;
exports.MediaPadSource = MediaPadSource;
