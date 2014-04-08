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
 * Filter that detects people agglomeration in video streams
 *
 * @class   module:kwsMediaApi/filters~CrowdDetectorFilter
 * @extends module:kwsMediaApi~Filter
 */

/**
 * Create a :rom:cls:`CrowdDetectorFilter`
 *
 * @constructor
 *
 * @param {string} id
 * @param {module:kwsMediaApi~MediaContainer} parent
 * @param {module:kwsMediaApi~MediaPipeline} pipeline
 * @param {module:kwsMediaApi/filters~CrowdDetectorFilter.constructorParams} params
 */
function CrowdDetectorFilter(id, parent, pipeline, params)
{
  Filter.call(this, id, parent, pipeline, params);
};
inherits(CrowdDetectorFilter, Filter);


/**
 * @type   module:kwsMediaApi/filters~CrowdDetectorFilter.constructorParams
 * @extend module:kwsMediaApi~Filter.constructorParams
 *
 * @property {MediaPipeline} mediaPipeline
 *  the :rom:cls:`MediaPipeline` to which the filter belongs
 *
 * @property {RegionOfInterest} rois
 *  Regions of interest for the filter
 */
CrowdDetectorFilter.constructorParams = {
  mediaPipeline: {
    type: 'MediaPipeline',
    required: true
  },

  rois: {
    type: 'RegionOfInterest',
    isList: true,
    required: true
  },
};
extend(CrowdDetectorFilter.constructorParams, Filter.constructorParams);

/**
 * @type   module:kwsMediaApi/filters~CrowdDetectorFilter.events
 * @extend module:kwsMediaApi~Filter.events
 */
CrowdDetectorFilter.events = ['CrowdDetectorDirection', 'CrowdDetectorFluidity', 'CrowdDetectorOccupancy'];
CrowdDetectorFilter.events.concat(Filter.events);


/**
 *
 *
 * @param {module:kwsMediaApi/filters~CrowdDetectorFilter.constructorParams} params
 */
CrowdDetectorFilter.create = function(pipeline, params, callback)
{
  pipeline.createMediaElement('CrowdDetectorFilter', params, callback);
};


module.exports = CrowdDetectorFilter;
