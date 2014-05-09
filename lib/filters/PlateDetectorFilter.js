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

var checkType = require('../checkType');


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
 */
function PlateDetectorFilter(id)
{
  Filter.call(this, id);
};
inherits(PlateDetectorFilter, Filter);


/**
 * Configures the average width of the license plates in the image represented as an image percentage.
 *
 * @param {float} plateWidthPercentage
 *  average width of the license plates represented as an image percentage [0..1].
 *
 * @param {module:kwsMediaApi/filters~PlateDetectorFilter.setPlateWidthPercentageCallback} [callback]
 *
 * @return {module:kwsMediaApi/filters~PlateDetectorFilter}
 *  The own media object
 */
PlateDetectorFilter.prototype.setPlateWidthPercentage = function(plateWidthPercentage, callback){
  checkType('float', 'plateWidthPercentage', plateWidthPercentage, {required: true});

  var params = {
    plateWidthPercentage: plateWidthPercentage,
  };

  this.invoke('setPlateWidthPercentage', params, callback);

  return this;
};
/**
 * @callback PlateDetectorFilter~setPlateWidthPercentageCallback
 * @param {Error} error
 */


/**
 * @type module:kwsMediaApi/filters~PlateDetectorFilter.constructorParams
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

/**
 * @type   module:kwsMediaApi/filters~PlateDetectorFilter.events
 * @extend module:kwsMediaApi~Filter.events
 */
PlateDetectorFilter.events = ['PlateDetected'];
PlateDetectorFilter.events.concat(Filter.events);


module.exports = PlateDetectorFilter;


PlateDetectorFilter.check = function(key, value)
{
  if(!(value instanceof PlateDetectorFilter))
    throw SyntaxError(key+' param should be a PlateDetectorFilter, not '+typeof value);
};
