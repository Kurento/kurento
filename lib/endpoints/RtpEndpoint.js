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
 * Endpoint that provides bidirectional content delivery capabilities with remote networked peers through RTP protocol. An :rom:cls:`RtpEndpoint` contains paired sink and source :rom:cls:`MediaPad` for audio and video.
 *
 * @class   module:kwsMediaApi/endpoints~RtpEndpoint
 * @extends module:kwsMediaApi~SdpEndpoint
 */

/**
 * Builder for the :rom:cls:`RtpEndpoint`
 *
 * @constructor
 *
 * @param {string} id
 */
function RtpEndpoint(id)
{
  SdpEndpoint.call(this, id);
};
inherits(RtpEndpoint, SdpEndpoint);


/**
 * @type module:kwsMediaApi/endpoints~RtpEndpoint.constructorParams
 *
 * @property {MediaPipeline} mediaPipeline
 *  the :rom:cls:`MediaPipeline` to which the endpoint belongs
 */
RtpEndpoint.constructorParams = {
  mediaPipeline: {
    type: 'MediaPipeline',
    required: true
  },
};

/**
 * @type   module:kwsMediaApi/endpoints~RtpEndpoint.events
 * @extend module:kwsMediaApi~SdpEndpoint.events
 */
RtpEndpoint.events = [];
RtpEndpoint.events.concat(SdpEndpoint.events);


module.exports = RtpEndpoint;


RtpEndpoint.check = function(key, value)
{
  if(!(value instanceof RtpEndpoint))
    throw SyntaxError(key+' param should be a RtpEndpoint, not '+typeof value);
};
