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

var MediaPad = require('./MediaPad');


/**
 * Special type of pad, used by a media element to generate a media stream.
 *
 * @abstract
 * @class   module:kwsMediaApi~MediaSource
 * @extends module:kwsMediaApi~MediaPad
 */

/**
 *
 * @constructor
 *
 * @param {string} id
 * @param {module:kwsMediaApi~MediaContainer} parent
 * @param {module:kwsMediaApi~MediaPipeline} pipeline
 * @param {module:kwsMediaApi~MediaElement} mediaElement - Element owner of this pad
 * @param {external:String} description - Description of this pad
 */
function MediaSource(id, parent, pipeline, mediaElement, description)
{
  MediaPad.call(this, id, parent, pipeline, mediaElement, description);


  var sinks = [];


  /**
   * Connects the current source with a :rom:cls:`MediaSink`
   *
   * @param {MediaSink} sink
   *  The sink to connect this source
   *
   * @param {module:kwsMediaApi~MediaSource.connectCallback} [callback]
   *
   * @return {module:kwsMediaApi~MediaSource}
   *  The own media object
   */
  this.connect = function(sink, callback){
    checkType('MediaSink', 'sink', sink, {required: true});

    var params = {
      sink: sink.id
    };

    callback = callback || function(){};

    this.invoke('connect', params, function(error)
    {
      if(error) return callback(error);

      sinks.push(sink);
      Object.defineProperty(sink, 'source', {value: this});
    });

    return this;
  };
  /**
   * @callback MediaSource~connectCallback
   * @param {Error} error
   */

  /**
   * Disconnect this source pad from the specified sink pad
   *
   * @public
   *
   * @param {...MediaPadSink} sink - Sink pad to be disconnected
   *
   * @return {MediaSource} The own pad
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

      sinks.splice(sinks.indexOf(sink, 1));
      Object.defineProperty(sink, 'source', {value: undefined});
    });

    return this;
  };

  /**
   * Gets all the :rom:cls:`MediaSinks<MediaSink>` to which this source is connected
   *
   * @public
   * @readonly
   * @member {module:kwsMediaApi~MediaPadSink[]} sinks
   * @memberof module:kwsMediaApi~MediaSource
   * @instance
   */
  this.__defineGetter__('sinks', function()
  {
    return sinks;
  });
};
inherits(MediaSource, MediaPad);


/**
 * @type   module:kwsMediaApi~MediaSource.constructorParams
 * @extend module:kwsMediaApi~MediaPad.constructorParams
 */
MediaSource.constructorParams = {};
extend(MediaSource.constructorParams, MediaPad.constructorParams);

/**
 * @type   module:kwsMediaApi~MediaSource.events
 * @extend module:kwsMediaApi~MediaPad.events
 */
MediaSource.events = [];
MediaSource.events.concat(MediaPad.events);


module.exports = MediaSource;
