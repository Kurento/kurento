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
 * @param {module:kwsMediaApi~MediaSource.constructorParams} params
 */
function MediaSource(id, parent, pipeline, params)
{
  MediaPad.call(this, id, parent, pipeline, params);
};
inherits(MediaSource, MediaPad);


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
MediaSource.prototype.connect = function(sink, callback){
  checkType('MediaSink', 'sink', sink, {required: true});

  var params = {
    sink: sink,
  };

  this.invoke('connect', params, callback);

  return this;
};
/**
 * @callback MediaSource~connectCallback
 * @param {Error} error
 */

/**
 * Gets all the :rom:cls:`MediaSinks<MediaSink>` to which this source is connected
 *
 * @param {module:kwsMediaApi~MediaSource.getConnectedSinksCallback} [callback]
 *
 * @return {module:kwsMediaApi~MediaSource}
 *  The own media object
 */
MediaSource.prototype.getConnectedSinks = function(callback){
  this.invoke('getConnectedSinks', callback);

  return this;
};
/**
 * @callback MediaSource~getConnectedSinksCallback
 * @param {Error} error
 * @param {MediaSink} result
 *  the list of sinks that the source is connected to
 */

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
