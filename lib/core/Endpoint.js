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
 * @module kwsMediaApi/core
 *
 * @copyright 2013-2014 Kurento (http://kurento.org/)
 * @license LGPL
 */

var MediaElement = require('./MediaElement');


/**
 * Base interface for all end points. An Endpoint is a :rom:cls:`MediaElement`
that allow :term:`KMS` to interchange media contents with external systems,
supporting different transport protocols and mechanisms, such as :term:`RTP`,
:term:`WebRTC`, :term:`HTTP`, ``file:/`` URLs... An ``Endpoint`` may
contain both sources and sinks for different media types, to provide
bidirectional communication.
 *
 * @abstract
 * @class   module:kwsMediaApi/core~Endpoint
 * @extends module:kwsMediaApi~MediaElement
 */

/**
 * @constructor
 *
 * @param {string} id
 */
function Endpoint(id)
{
  MediaElement.call(this, id);
};
inherits(Endpoint, MediaElement);


/**
 * @type module:kwsMediaApi/core~Endpoint.constructorParams
 */
Endpoint.constructorParams = {};

/**
 * @type   module:kwsMediaApi/core~Endpoint.events
 * @extend module:kwsMediaApi~MediaElement.events
 */
Endpoint.events = [];
Endpoint.events.concat(MediaElement.events);


module.exports = Endpoint;
