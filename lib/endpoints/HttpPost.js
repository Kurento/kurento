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

var Http = require('./Http');


/**
 * @class
 * @extends module:KwsMedia/elements~Http
 *
 * @param objectRef
 * @param {module:KwsMedia~MediaContainer} parent
 * @param {module:KwsMedia~MediaPipeline} pipeline
 * @param {module:KwsMedia/elements~Http.ConstructorParams} params
 */
function HttpPost(objectRef, parent, pipeline, params)
{
  Http.call(this, objectRef, parent, pipeline, params);
};
HttpPost.prototype.__proto__   = Http.prototype;
HttpPost.prototype.constructor = HttpPost;


/**
 * @see {@link module:KwsMedia/elements~Http.checkparams}
 */
HttpPost.checkparams = Http.checkparams;


module.exports = HttpPost;