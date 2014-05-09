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

var checkType = require('../checkType');


/**
 * Media API for the Kurento Web SDK
 *
 * @module kwsMediaApi/endpoints
 *
 * @copyright 2013-2014 Kurento (http://kurento.org/)
 * @license LGPL
 */

var SessionEndpoint = require('./SessionEndpoint');


/**
 * Endpoint that enables Kurento to work as an HTTP server, allowing peer HTTP clients to access media.
 *
 * @abstract
 * @class   module:kwsMediaApi/endpoints~HttpEndpoint
 * @extends module:kwsMediaApi~SessionEndpoint
 */

/**
 * @constructor
 *
 * @param {string} id
 */
function HttpEndpoint(id)
{
  SessionEndpoint.call(this, id);
};
inherits(HttpEndpoint, SessionEndpoint);


/**
 * Obtains the URL associated to this endpoint
 *
 * @param {module:kwsMediaApi/endpoints~HttpEndpoint.getUrlCallback} [callback]
 *
 * @return {module:kwsMediaApi/endpoints~HttpEndpoint}
 *  The own media object
 */
HttpEndpoint.prototype.getUrl = function(callback){
  this.invoke('getUrl', callback);

  return this;
};
/**
 * @callback HttpEndpoint~getUrlCallback
 * @param {Error} error
 * @param {String} result
 *  The url as a String
 */


/**
 * @type module:kwsMediaApi/endpoints~HttpEndpoint.constructorParams
 */
HttpEndpoint.constructorParams = {};

/**
 * @type   module:kwsMediaApi/endpoints~HttpEndpoint.events
 * @extend module:kwsMediaApi~SessionEndpoint.events
 */
HttpEndpoint.events = [];
HttpEndpoint.events.concat(SessionEndpoint.events);


module.exports = HttpEndpoint;
