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
 * An :rom:cls:`HttpPostEndpoint` contains SINK pads for AUDIO and VIDEO, which provide access to an HTTP file upload function

   This type of endpoint provide unidirectional communications. Its :rom:cls:`MediaSources <MediaSource>` are accessed through the :term:`HTTP` POST method.
 *
 * @class   module:kwsMediaApi/endpoints~HttpPostEndpoint
 * @extends module:kwsMediaApi~HttpEndpoint
 */

/**
 * Builder for the :rom:cls:`HttpPostEndpoint`.
 *
 * @constructor
 *
 * @param {string} id
 */
function HttpPostEndpoint(id)
{
  HttpEndpoint.call(this, id);
};
inherits(HttpPostEndpoint, HttpEndpoint);


/**
 * @type module:kwsMediaApi/endpoints~HttpPostEndpoint.constructorParams
 *
 * @property {int} [disconnectionTimeout]
 *  This is the time that an http endpoint will wait for a reconnection, in case an HTTP connection is lost.
 *
 * @property {MediaPipeline} mediaPipeline
 *  the :rom:cls:`MediaPipeline` to which the endpoint belongs
 *
 * @property {boolean} [useEncodedMedia]
 *  configures the endpoint to use encoded media instead of raw media. If the parameter is not set then the element uses raw media. Changing this parameter could affect in a severe way to stability because key frames lost will not be generated. Changing the media type does not affect to the result except in the performance (just in the case where original media and target media are the same) and in the problem with the key frames. We strongly recommended not to use this parameter because correct behaviour is not guarantied.
 */
HttpPostEndpoint.constructorParams = {
  disconnectionTimeout: {
    type: 'int',
  },

  mediaPipeline: {
    type: 'MediaPipeline',
    required: true
  },

  useEncodedMedia: {
    type: 'boolean',
  },
};

/**
 * @type   module:kwsMediaApi/endpoints~HttpPostEndpoint.events
 * @extend module:kwsMediaApi~HttpEndpoint.events
 */
HttpPostEndpoint.events = ['EndOfStream'];
HttpPostEndpoint.events.concat(HttpEndpoint.events);


module.exports = HttpPostEndpoint;


HttpPostEndpoint.check = function(key, value)
{
  if(!(value instanceof HttpPostEndpoint))
    throw SyntaxError(key+' param should be a HttpPostEndpoint, not '+typeof value);
};
