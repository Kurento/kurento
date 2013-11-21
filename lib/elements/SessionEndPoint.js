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

var MediaElement = require('../MediaElement');


/**
 * @class
 * @extends module:KwsMedia~MediaElement
 *
 * @param objectRef
 * @param parent
 * @param pipeline
 * @param {module:KwsMedia/elements~MediaElement.ConstructorParams} params
 */
function SessionEndPoint(objectRef, parent, pipeline, params)
{
  MediaElement.call(this, objectRef, parent, pipeline, params);
};
SessionEndPoint.prototype.__proto__   = MediaElement.prototype;
SessionEndPoint.prototype.constructor = SessionEndPoint;


/**
 * 
 * @param {module:KwsMedia/elements~MediaObjectConstructorParams} params
 *
 * @throws {SyntaxError}
 *
 * @returns {module:KwsMedia/elements~MediaObjectConstructorParams}
 */
SessionEndPoint.checkparams = function(params)
{
  return MediaElement.checkparams(params);
};


module.exports = SessionEndPoint;