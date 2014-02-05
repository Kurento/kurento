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

var Uri = require('./Uri');


/**
 * @class
 * @extends module:KwsMedia/endpoints~Player
 *
 * @param id
 * @param {module:KwsMedia~MediaContainer} parent
 * @param {module:KwsMedia~MediaPipeline} pipeline
 * @param {module:KwsMedia/endpoints~Uri.ConstructorParams} params
 */
function Player(id, parent, pipeline, params)
{
	Uri.call(this, id, parent, pipeline, params);
};
Player.prototype.__proto__   = Uri.prototype;
Player.prototype.constructor = Player;


/**
 * @see {@link module:KwsMedia/endpoints~Uri.paramsScheme}
 */
Player.paramsScheme = Uri.paramsScheme;


Player.create = function(pipeline, params, callback)
{
  pipeline.createMediaElement('PlayerEndpoint', params, callback);
};


module.exports = Player;
