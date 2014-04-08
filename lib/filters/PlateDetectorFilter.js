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
 * PlateDetectorFilter interface. This type of :rom:cls:`Endpoint` detects
vehicle plates in a video feed.
 *
 * @class   module:kwsMediaApi/filters~PlateDetectorFilter
 * @extends module:kwsMediaApi~Filter
 */

/**
 * Create a :rom:cls:`PlateDetectorFilter` for the given :rom:cls:`MediaPipeline`
 *
 * @constructor
 *
 * @param {string} id
 * @param {module:kwsMediaApi~MediaContainer} parent
 * @param {module:kwsMediaApi~MediaPipeline} pipeline
 * @param {module:kwsMediaApi/filters~PlateDetectorFilter.constructorParams} params
 */
function PlateDetectorFilter(id, parent, pipeline, params)
{
  Filter.call(this, id, parent, pipeline, params);
};
inherits(PlateDetectorFilter, Filter);


/**
 * @type   module:kwsMediaApi/filters~PlateDetectorFilter.constructorParams
 * @extend module:kwsMediaApi~Filter.constructorParams
 *
 * @property {MediaPipeline} mediaPipeline
 *  the parent :rom:cls:`MediaPipeline` of this :rom:cls:`PlateDetectorFilter`
 */
PlateDetectorFilter.constructorParams = {
  mediaPipeline: {
    type: 'MediaPipeline',
    required: true
  },
};
extend(PlateDetectorFilter.constructorParams, Filter.constructorParams);

/**
 * @type   module:kwsMediaApi/filters~PlateDetectorFilter.events
 * @extend module:kwsMediaApi~Filter.events
 */
PlateDetectorFilter.events = ['PlateDetected'];
PlateDetectorFilter.events.concat(Filter.events);


/**
 *
 *
 * @param {module:kwsMediaApi/filters~PlateDetectorFilter.constructorParams} params
 */
PlateDetectorFilter.create = function(pipeline, params, callback)
{
  pipeline.createMediaElement('PlateDetectorFilter', params, callback);
};


module.exports = PlateDetectorFilter;
