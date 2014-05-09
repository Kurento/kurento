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
 * @module kwsMediaApi/filters
 *
 * @copyright 2013-2014 Kurento (http://kurento.org/)
 * @license LGPL
 */

var Filter = require('../core/Filter');


/**
 * Filter that detects faces in a video feed. Those on the right half of the feed are overlaid with a pirate hat, and those on the left half are covered by a Darth Vader helmet. This is an example filter, intended to demonstrate how to integrate computer vision capabilities into the multimedia infrastructure.
 *
 * @class   module:kwsMediaApi/filters~JackVaderFilter
 * @extends module:kwsMediaApi~Filter
 */

/**
 * Create a new :rom:cls:`Filter`
 *
 * @constructor
 *
 * @param {string} id
 */
function JackVaderFilter(id)
{
  Filter.call(this, id);
};
inherits(JackVaderFilter, Filter);


/**
 * @type module:kwsMediaApi/filters~JackVaderFilter.constructorParams
 *
 * @property {MediaPipeline} mediaPipeline
 *  the :rom:cls:`MediaPipeline` to which the filter belongs
 */
JackVaderFilter.constructorParams = {
  mediaPipeline: {
    type: 'MediaPipeline',
    required: true
  },
};

/**
 * @type   module:kwsMediaApi/filters~JackVaderFilter.events
 * @extend module:kwsMediaApi~Filter.events
 */
JackVaderFilter.events = [];
JackVaderFilter.events.concat(Filter.events);


module.exports = JackVaderFilter;


JackVaderFilter.check = function(key, value)
{
  if(!(value instanceof JackVaderFilter))
    throw SyntaxError(key+' param should be a JackVaderFilter, not '+typeof value);
};
