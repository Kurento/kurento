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

var inherits = require('inherits');


/**
 * Media API for the Kurento Web SDK
 *
 * @module kwsMediaApi/core
 *
 * @copyright 2013-2014 Kurento (http://kurento.org/)
 * @license LGPL
 */

var MediaElement = require('./MediaElement');


/**
 * Base interface for all filters. This is a certain type of :rom:cls:`MediaElement`, that processes media injected through its :rom:cls:`MediaSink`, and delivers the outcome through its :rom:cls:`MediaSource`.
 *
 * @abstract
 * @class   module:kwsMediaApi/core~Filter
 * @extends module:kwsMediaApi~MediaElement
 */

/**
 * @constructor
 *
 * @param {string} id
 */
function Filter(id)
{
  MediaElement.call(this, id);
};
inherits(Filter, MediaElement);


/**
 * @type module:kwsMediaApi/core~Filter.constructorParams
 */
Filter.constructorParams = {};

/**
 * @type   module:kwsMediaApi/core~Filter.events
 * @extend module:kwsMediaApi~MediaElement.events
 */
Filter.events = [];
Filter.events.concat(MediaElement.events);


module.exports = Filter;
