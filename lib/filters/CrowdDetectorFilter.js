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
 */
function CrowdDetectorFilter(id)
{
  Filter.call(this, id);
};
inherits(CrowdDetectorFilter, Filter);


/**
 * @type module:kwsMediaApi/filters~CrowdDetectorFilter.constructorParams
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

/**
 * @type   module:kwsMediaApi/filters~CrowdDetectorFilter.events
 * @extend module:kwsMediaApi~Filter.events
 */
CrowdDetectorFilter.events = ['CrowdDetectorDirection', 'CrowdDetectorFluidity', 'CrowdDetectorOccupancy'];
CrowdDetectorFilter.events.concat(Filter.events);


module.exports = CrowdDetectorFilter;


CrowdDetectorFilter.check = function(key, value)
{
  if(!(value instanceof CrowdDetectorFilter))
    throw SyntaxError(key+' param should be a CrowdDetectorFilter, not '+typeof value);
};
