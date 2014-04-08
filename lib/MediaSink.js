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
 * Special type of pad, used by a :rom:cls:`MediaElement` to receive a media stream.
 *
 * @abstract
 * @class   module:kwsMediaApi~MediaSink
 * @extends module:kwsMediaApi~MediaPad
 */

/**
 *
 * @constructor
 *
 * @param {string} id
 * @param {module:kwsMediaApi~MediaContainer} parent
 * @param {module:kwsMediaApi~MediaPipeline} pipeline
 * @param {module:kwsMediaApi~MediaSink.constructorParams} params
 */
function MediaSink(id, parent, pipeline, params)
{
  MediaPad.call(this, id, parent, pipeline, params);
};
inherits(MediaSink, MediaPad);


/**
 * Disconnects the current sink from the referred :rom:cls:`MediaSource`
 *
 * @param {MediaSource} src
 *  The source to disconnect
 *
 * @param {module:kwsMediaApi~MediaSink.disconnectCallback} [callback]
 *
 * @return {module:kwsMediaApi~MediaSink}
 *  The own media object
 */
MediaSink.prototype.disconnect = function(src, callback){
  checkType('MediaSource', 'src', src, {required: true});

  var params = {
    src: src,
  };

  this.invoke('disconnect', params, callback);

  return this;
};
/**
 * @callback MediaSink~disconnectCallback
 * @param {Error} error
 */

/**
 * Gets the :rom:cls:`MediaSource` that is connected to this sink.
 *
 * @param {module:kwsMediaApi~MediaSink.getConnectedSrcCallback} [callback]
 *
 * @return {module:kwsMediaApi~MediaSink}
 *  The own media object
 */
MediaSink.prototype.getConnectedSrc = function(callback){
  this.invoke('getConnectedSrc', callback);

  return this;
};
/**
 * @callback MediaSink~getConnectedSrcCallback
 * @param {Error} error
 * @param {MediaSource} result
 *  The source connected to this sink
 */

/**
 * @type   module:kwsMediaApi~MediaSink.constructorParams
 * @extend module:kwsMediaApi~MediaPad.constructorParams
 */
MediaSink.constructorParams = {};
extend(MediaSink.constructorParams, MediaPad.constructorParams);

/**
 * @type   module:kwsMediaApi~MediaSink.events
 * @extend module:kwsMediaApi~MediaPad.events
 */
MediaSink.events = [];
MediaSink.events.concat(MediaPad.events);


module.exports = MediaSink;
