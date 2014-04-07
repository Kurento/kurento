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

/**
 * Media API for the Kurento Web SDK
 *
 * @module kwsMediaApi/endpoints
 *
 * @copyright 2014 Kurento (http://kurento.org/)
 * @license LGPL
 */

var Http = require('./Http');

var extend = require('extend');
var inherits = require('inherits');


var checkType = require('../checkType');


/**
 * @class
 * @extends module:kwsMediaApi/endpoints~Http
 *
 * @param id
 * @param parent
 * @param pipeline
 * @param {MediaHttpGetConstructorParams} params
 */
function HttpGet(id, parent, pipeline, params)
{
  Http.call(this, id, parent, pipeline, params);
};
inherits(HttpGet, Http);


HttpGet.paramsScheme = {
  disconnectionTimeout:
  {
    type: 'int'
  },

  mediaPipeline: {
    type: 'MediaPipeline',
    required: true
  },

  mediaProfile:
  {
    type: 'MediaProfileSpecType'
  },

  terminateOnEOS:
  {
    type: 'boolean'
  }
};
extend(HttpGet.paramsScheme, Http.paramsScheme);


HttpGet.create = function(pipeline, params, callback)
{
  pipeline.createMediaElement('HttpGetEndpoint', params, callback);
};


module.exports = HttpGet;
