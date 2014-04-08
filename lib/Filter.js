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


/**
 * Media API for the Kurento Web SDK
 *
 * @module kwsMediaApi
 *
 * @copyright 2013-2014 Kurento (http://kurento.org/)
 * @license LGPL
 */

var MediaElement = require('./MediaElement');


/**
 * Base interface for all filters. This is a certain type of :rom:cls:`MediaElement`, that processes media injected through its :rom:cls:`MediaSink`, and delivers the outcome through its :rom:cls:`MediaSource`.
 *
 * @abstract
 * @class   module:kwsMediaApi~Filter
 * @extends module:kwsMediaApi~MediaElement
 */

/**
 *
 * @constructor
 *
 * @param {string} id
 * @param {module:kwsMediaApi~MediaContainer} parent
 * @param {module:kwsMediaApi~MediaPipeline} pipeline
 * @param {module:kwsMediaApi~Filter.constructorParams} params
 */
function Filter(id, parent, pipeline, params)
{
  // Bubble the event for new MediaObjects
  this.on('mediaObject', function(mediaObject)
  {
    parent.emit('mediaObject', mediaObject);
  });

  MediaElement.call(this, id, parent, pipeline, params);
};
inherits(Filter, MediaElement);


/**
 * @type   module:kwsMediaApi~Filter.constructorParams
 * @extend module:kwsMediaApi~MediaElement.constructorParams
 */
Filter.constructorParams = {};
extend(Filter.constructorParams, MediaElement.constructorParams);

/**
 * @type   module:kwsMediaApi~Filter.events
 * @extend module:kwsMediaApi~MediaElement.events
 */
Filter.events = [];
Filter.events.concat(MediaElement.events);


module.exports = Filter;
