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

var extend   = require('extend');
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
 * @param {module:kwsMediaApi~MediaContainer} parent
 * @param {module:kwsMediaApi~MediaPipeline} pipeline
 * @param {module:kwsMediaApi/endpoints~RtpEndpoint.constructorParams} params
 */
function RtpEndpoint(id, parent, pipeline, params)
{
  SdpEndpoint.call(this, id, parent, pipeline, params);
};
inherits(RtpEndpoint, SdpEndpoint);


/**
 * @type   module:kwsMediaApi/endpoints~RtpEndpoint.constructorParams
 * @extend module:kwsMediaApi~SdpEndpoint.constructorParams
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
extend(RtpEndpoint.constructorParams, SdpEndpoint.constructorParams);

/**
 * @type   module:kwsMediaApi/endpoints~RtpEndpoint.events
 * @extend module:kwsMediaApi~SdpEndpoint.events
 */
RtpEndpoint.events = [];
RtpEndpoint.events.concat(SdpEndpoint.events);


/**
 *
 *
 * @param {module:kwsMediaApi/endpoints~RtpEndpoint.constructorParams} params
 */
RtpEndpoint.create = function(pipeline, params, callback)
{
  pipeline.createMediaElement('RtpEndpoint', params, callback);
};


module.exports = RtpEndpoint;
