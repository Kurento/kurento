/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

var MediaFilter = require('../MediaFilter');


/**
 * @class   module:kwsMediaApi/filters~GStreamer
 * @extends module:kwsMediaApi~MediaFilter
 *
 * @param id
 * @param parent
 * @param pipeline
 * @param {module:kwsMediaApi/filters~GStreamer.ConstructorParams} params
 */
function GStreamer(id, parent, pipeline, params)
{
  MediaFilter.call(this, id, parent, pipeline, params);
};
inherits(GStreamer, MediaFilter);


/**
 * @type   module:kwsMediaApi/filters~GStreamer.paramsScheme
 * @extend module:kwsMediaApi~MediaFilter.paramsScheme
 */
GStreamer.paramsScheme =
{
  /**
   * @type String
   */
  command:
  {
    type: 'String',
    required: true
  }
};
extend(GStreamer.paramsScheme, MediaFilter.paramsScheme);


GStreamer.create = function(pipeline, params, callback)
{
  pipeline.createMediaElement('GStreamerFilter', params, callback);
};


module.exports = GStreamer;
