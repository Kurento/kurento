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

var SdpEndPoint = require('./SdpEndPoint');


/**
 * @class
 * @extends module:KwsMedia/elements~SdpEndPoint
 *
 * @param objectRef
 * @param parent
 * @param pipeline
 * @param {MediaHttpEndPointConstructorParams} params
 */
function RtpEndPoint(objectRef, parent, pipeline, params)
{
  SdpEndPoint.call(this, objectRef, parent, pipeline, params);
};
RtpEndPoint.prototype.__proto__   = SdpEndPoint.prototype;
RtpEndPoint.prototype.constructor = RtpEndPoint;


/**
 * 
 * @param {SdpEndPointConstructorParams} params
 *
 * @throws {SyntaxError}
 *
 * @returns {SdpEndPointConstructorParams}
 */
RtpEndPoint.checkparams = function(params)
{
  /**
   * @type {SdpEndPointConstructorParams}
   */
  return SdpEndPoint.checkparams(params);
};


module.exports = RtpEndPoint;