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


module.exports = MediaPad;
