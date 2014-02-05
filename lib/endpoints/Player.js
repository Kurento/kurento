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
 * @module KwsMedia/elements
 *
 * @copyright 2014 Kurento (http://kurento.org/)
 * @license LGPL
 */

var Uri = require('./Uri');


/**
 * @class
 * @extends module:KwsMedia/elements~Player
 *
 * @param objectRef
 * @param {module:KwsMedia~MediaContainer} parent
 * @param {module:KwsMedia~MediaPipeline} pipeline
 * @param {module:KwsMedia/elements~Uri.ConstructorParams} params
 */
function Player(objectRef, parent, pipeline, params)
{
	Uri.call(this, objectRef, parent, pipeline, params);
};
Player.prototype.__proto__   = Uri.prototype;
Player.prototype.constructor = Player;


/**
 * @see {@link module:KwsMedia/elements~Uri.checkparams}
 */
Player.checkparams = Uri.checkparams;


module.exports = Player;