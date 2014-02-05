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

var Sdp = require('./Sdp');


/**
 * @class
 * @extends module:KwsMedia/endpoints~Sdp
 *
 * @param id
 * @param {module:KwsMedia~MediaContainer} parent
 * @param {module:KwsMedia~MediaPipeline} pipeline
 * @param {module:KwsMedia/endpoints~Sdp.paramsScheme} params
 */
function WebRtc(id, parent, pipeline, params)
{
  Sdp.call(this, id, parent, pipeline, params);
};
WebRtc.prototype.__proto__   = Sdp.prototype;
WebRtc.prototype.constructor = WebRtc;


/**
 * @see {@link module:KwsMedia/endpoints~Sdp.paramsScheme}
 */
WebRtc.paramsScheme = Sdp.paramsScheme;


WebRtc.create = function(pipeline, params, callback)
{
  pipeline.createMediaElement('WebRtcEndpoint', params, callback);
};

module.exports = WebRtc;
