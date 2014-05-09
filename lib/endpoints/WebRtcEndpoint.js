/*
 * (C) Copyright 2013-2014 Kurento (http://kurento.org/)
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

var inherits = require('inherits');


/**
 * Media API for the Kurento Web SDK
 *
 * @module kwsMediaApi/endpoints
 *
 * @copyright 2013-2014 Kurento (http://kurento.org/)
 * @license LGPL
 */

var SdpEndpoint = require('./SdpEndpoint');


/**
 * WebRtcEndpoint interface. This type of ``Endpoint`` offers media streaming using WebRTC.
 *
 * @class   module:kwsMediaApi/endpoints~WebRtcEndpoint
 * @extends module:kwsMediaApi~SdpEndpoint
 */

/**
 * Builder for the :rom:cls:`WebRtcEndpoint`
 *
 * @constructor
 *
 * @param {string} id
 */
function WebRtcEndpoint(id)
{
  SdpEndpoint.call(this, id);
};
inherits(WebRtcEndpoint, SdpEndpoint);


/**
 * @type module:kwsMediaApi/endpoints~WebRtcEndpoint.constructorParams
 *
 * @property {MediaPipeline} mediaPipeline
 *  the :rom:cls:`MediaPipeline` to which the endpoint belongs
 */
WebRtcEndpoint.constructorParams = {
  mediaPipeline: {
    type: 'MediaPipeline',
    required: true
  },
};

/**
 * @type   module:kwsMediaApi/endpoints~WebRtcEndpoint.events
 * @extend module:kwsMediaApi~SdpEndpoint.events
 */
WebRtcEndpoint.events = [];
WebRtcEndpoint.events.concat(SdpEndpoint.events);


module.exports = WebRtcEndpoint;


WebRtcEndpoint.check = function(key, value)
{
  if(!(value instanceof WebRtcEndpoint))
    throw SyntaxError(key+' param should be a WebRtcEndpoint, not '+typeof value);
};
