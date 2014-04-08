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
 * @param {module:kwsMediaApi~MediaContainer} parent
 * @param {module:kwsMediaApi~MediaPipeline} pipeline
 * @param {module:kwsMediaApi/endpoints~WebRtcEndpoint.constructorParams} params
 */
function WebRtcEndpoint(id, parent, pipeline, params)
{
  SdpEndpoint.call(this, id, parent, pipeline, params);
};
inherits(WebRtcEndpoint, SdpEndpoint);


/**
 * @type   module:kwsMediaApi/endpoints~WebRtcEndpoint.constructorParams
 * @extend module:kwsMediaApi~SdpEndpoint.constructorParams
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
extend(WebRtcEndpoint.constructorParams, SdpEndpoint.constructorParams);

/**
 * @type   module:kwsMediaApi/endpoints~WebRtcEndpoint.events
 * @extend module:kwsMediaApi~SdpEndpoint.events
 */
WebRtcEndpoint.events = [];
WebRtcEndpoint.events.concat(SdpEndpoint.events);


/**
 *
 *
 * @param {module:kwsMediaApi/endpoints~WebRtcEndpoint.constructorParams} params
 */
WebRtcEndpoint.create = function(pipeline, params, callback)
{
  pipeline.createMediaElement('WebRtcEndpoint', params, callback);
};


module.exports = WebRtcEndpoint;
