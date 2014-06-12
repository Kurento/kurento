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

var Endpoint = require('../core/Endpoint');


/**
 * Session based endpoint. A session is considered to be started when the media exchange starts. On the other hand, sessions terminate when a timeout, defined by the developer, takes place after the connection is lost.
 *
 * @abstract
 * @class   module:kwsMediaApi/endpoints~SessionEndpoint
 * @extends module:kwsMediaApi~Endpoint
 */

/**
 * @constructor
 *
 * @param {string} id
 */
function SessionEndpoint(id)
{
  Endpoint.call(this, id);
};
inherits(SessionEndpoint, Endpoint);


/**
 * @type module:kwsMediaApi/endpoints~SessionEndpoint.constructorParams
 */
SessionEndpoint.constructorParams = {};

/**
 * @type   module:kwsMediaApi/endpoints~SessionEndpoint.events
 * @extend module:kwsMediaApi~Endpoint.events
 */
SessionEndpoint.events = ['MediaSessionStarted', 'MediaSessionTerminated'];
SessionEndpoint.events.concat(Endpoint.events);


module.exports = SessionEndpoint;


SessionEndpoint.check = function(key, value)
{
  if(!(value instanceof SessionEndpoint))
    throw SyntaxError(key+' param should be a SessionEndpoint, not '+typeof value);
};
