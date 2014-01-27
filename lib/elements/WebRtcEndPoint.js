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
 * New node file
 */

var SdpEndPoint = require('./SdpEndPoint');


function WebRtcEndPoint(objectRef, parent, pipeline, params)
{
  SdpEndPoint.call(this, objectRef, parent, pipeline, params);
};
WebRtcEndPoint.prototype.__proto__   = SdpEndPoint.prototype;
WebRtcEndPoint.prototype.constructor = WebRtcEndPoint;


/**
 * 
 * @param {SdpEndPointConstructorParams} params
 *
 * @throws {SyntaxError}
 *
 * @returns {SdpEndPointConstructorParams}
 */
WebRtcEndPoint.checkparams = function(params)
{
  /**
   * @type {SdpEndPointConstructorParams}
   */
  return SdpEndPoint.checkparams(params);
};


module.exports = WebRtcEndPoint;