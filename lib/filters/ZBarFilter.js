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
 * This filter detects :term:`QR` codes in a video feed. When a code is found, the filter raises a :rom:evnt:`CodeFound` event.
 *
 * @class   module:kwsMediaApi/filters~ZBarFilter
 * @extends module:kwsMediaApi~Filter
 */

/**
 * Builder for the :rom:cls:`ZBarFilter`.
 *
 * @constructor
 *
 * @param {string} id
 */
function ZBarFilter(id)
{
  Filter.call(this, id);
};
inherits(ZBarFilter, Filter);


/**
 * @type module:kwsMediaApi/filters~ZBarFilter.constructorParams
 *
 * @property {MediaPipeline} mediaPipeline
 *  the :rom:cls:`MediaPipeline` to which the filter belongs
 */
ZBarFilter.constructorParams = {
  mediaPipeline: {
    type: 'MediaPipeline',
    required: true
  },
};

/**
 * @type   module:kwsMediaApi/filters~ZBarFilter.events
 * @extend module:kwsMediaApi~Filter.events
 */
ZBarFilter.events = ['CodeFound'];
ZBarFilter.events.concat(Filter.events);


module.exports = ZBarFilter;


ZBarFilter.check = function(key, value)
{
  if(!(value instanceof ZBarFilter))
    throw SyntaxError(key+' param should be a ZBarFilter, not '+typeof value);
};
