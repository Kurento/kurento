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
 * @module KwsMedia/endpoints
 *
 * @copyright 2014 Kurento (http://kurento.org/)
 * @license LGPL
 */

var Http = require('./Http');

var inherits = require('inherits');


/**
 * @class
 * @extends module:KwsMedia/endpoints~Http
 *
 * @param id
 * @param {module:KwsMedia~MediaContainer} parent
 * @param {module:KwsMedia~MediaPipeline} pipeline
 * @param {module:KwsMedia/endpoints~Http.paramsScheme} params
 */
function HttpPost(id, parent, pipeline, params)
{
  Http.call(this, id, parent, pipeline, params);
};
inherits(HttpPost, Http);


/**
 * @see {@link module:KwsMedia/endpoints~Http.paramsScheme}
 */
HttpPost.paramsScheme = Http.paramsScheme;


HttpPost.create = function(pipeline, params, callback)
{
  pipeline.createMediaElement('HttpPostEndpoint', params, callback);
};


module.exports = HttpPost;
