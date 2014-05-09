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

var HttpEndpoint = require('./HttpEndpoint');


/**
 * An ``HttpGetEndpoint`` contains SOURCE pads for AUDIO and VIDEO, delivering media using HTML5 pseudo-streaming mechanism.

   This type of endpoint provide unidirectional communications. Its :rom:cls:`MediaSink` is associated with the HTTP GET method
 *
 * @class   module:kwsMediaApi/endpoints~HttpGetEndpoint
 * @extends module:kwsMediaApi~HttpEndpoint
 */

/**
 * Builder for the :rom:cls:`HttpGetEndpoint`.
 *
 * @constructor
 *
 * @param {string} id
 */
function HttpGetEndpoint(id)
{
  HttpEndpoint.call(this, id);
};
inherits(HttpGetEndpoint, HttpEndpoint);


/**
 * @type module:kwsMediaApi/endpoints~HttpGetEndpoint.constructorParams
 *
 * @property {int} [disconnectionTimeout]
 *  disconnection timeout in seconds.

This is the time that an http endpoint will wait for a reconnection, in case an HTTP connection is lost.
 *
 * @property {MediaPipeline} mediaPipeline
 *  the :rom:cls:`MediaPipeline` to which the endpoint belongs
 *
 * @property {MediaProfileSpecType} [mediaProfile]
 *  the :rom:enum:`MediaProfileSpecType` (WEBM, MP4...) for the endpoint
 *
 * @property {boolean} [terminateOnEOS]
 *  raise a :rom:evnt:`MediaSessionTerminated` event when the associated player raises a :rom:evnt:`EndOfStream`, and thus terminate the media session
 */
HttpGetEndpoint.constructorParams = {
  disconnectionTimeout: {
    type: 'int',
  },

  mediaPipeline: {
    type: 'MediaPipeline',
    required: true
  },

  mediaProfile: {
    type: 'MediaProfileSpecType',
  },

  terminateOnEOS: {
    type: 'boolean',
  },
};

/**
 * @type   module:kwsMediaApi/endpoints~HttpGetEndpoint.events
 * @extend module:kwsMediaApi~HttpEndpoint.events
 */
HttpGetEndpoint.events = [];
HttpGetEndpoint.events.concat(HttpEndpoint.events);


module.exports = HttpGetEndpoint;


HttpGetEndpoint.check = function(key, value)
{
  if(!(value instanceof HttpGetEndpoint))
    throw SyntaxError(key+' param should be a HttpGetEndpoint, not '+typeof value);
};
