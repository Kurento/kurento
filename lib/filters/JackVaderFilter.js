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
 * @module kwsMediaApi/filters
 *
 * @copyright 2013-2014 Kurento (http://kurento.org/)
 * @license LGPL
 */

var Filter = require('../Filter');


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
 * @param {module:kwsMediaApi~MediaContainer} parent
 * @param {module:kwsMediaApi~MediaPipeline} pipeline
 * @param {module:kwsMediaApi/filters~JackVaderFilter.constructorParams} params
 */
function JackVaderFilter(id, parent, pipeline, params)
{
  Filter.call(this, id, parent, pipeline, params);
};
inherits(JackVaderFilter, Filter);


/**
 * @type   module:kwsMediaApi/filters~JackVaderFilter.constructorParams
 * @extend module:kwsMediaApi~Filter.constructorParams
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
extend(JackVaderFilter.constructorParams, Filter.constructorParams);

/**
 * @type   module:kwsMediaApi/filters~JackVaderFilter.events
 * @extend module:kwsMediaApi~Filter.events
 */
JackVaderFilter.events = [];
JackVaderFilter.events.concat(Filter.events);


/**
 *
 *
 * @param {module:kwsMediaApi/filters~JackVaderFilter.constructorParams} params
 */
JackVaderFilter.create = function(pipeline, params, callback)
{
  pipeline.createMediaElement('JackVaderFilter', params, callback);
};


module.exports = JackVaderFilter;
