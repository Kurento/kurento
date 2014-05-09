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
 * This is a generic filter interface, that creates GStreamer filters in the media server.
 *
 * @class   module:kwsMediaApi/filters~GStreamerFilter
 * @extends module:kwsMediaApi~Filter
 */

/**
 * Create a :rom:cls:`GStreamerFilter`
 *
 * @constructor
 *
 * @param {string} id
 */
function GStreamerFilter(id)
{
  Filter.call(this, id);
};
inherits(GStreamerFilter, Filter);


/**
 * @type module:kwsMediaApi/filters~GStreamerFilter.constructorParams
 *
 * @property {String} command
 *  command that would be used to instantiate the filter, as in `gst-launch <http://rpm.pbone.net/index.php3/stat/45/idpl/19531544/numer/1/nazwa/gst-launch-1.0>`__
 *
 * @property {MediaPipeline} mediaPipeline
 *  the :rom:cls:`MediaPipeline` to which the filter belongs
 */
GStreamerFilter.constructorParams = {
  command: {
    type: 'String',
    required: true
  },

  mediaPipeline: {
    type: 'MediaPipeline',
    required: true
  },
};

/**
 * @type   module:kwsMediaApi/filters~GStreamerFilter.events
 * @extend module:kwsMediaApi~Filter.events
 */
GStreamerFilter.events = [];
GStreamerFilter.events.concat(Filter.events);


module.exports = GStreamerFilter;


GStreamerFilter.check = function(key, value)
{
  if(!(value instanceof GStreamerFilter))
    throw SyntaxError(key+' param should be a GStreamerFilter, not '+typeof value);
};
