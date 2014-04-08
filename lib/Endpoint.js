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
 * @module kwsMediaApi
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
 * @class   module:kwsMediaApi~Endpoint
 * @extends module:kwsMediaApi~MediaElement
 */

/**
 *
 * @constructor
 *
 * @param {string} id
 * @param {module:kwsMediaApi~MediaContainer} parent
 * @param {module:kwsMediaApi~MediaPipeline} pipeline
 * @param {module:kwsMediaApi~Endpoint.constructorParams} params
 */
function Endpoint(id, parent, pipeline, params)
{
  MediaElement.call(this, id, parent, pipeline, params);
};
inherits(Endpoint, MediaElement);


/**
 * @type   module:kwsMediaApi~Endpoint.constructorParams
 * @extend module:kwsMediaApi~MediaElement.constructorParams
 */
Endpoint.constructorParams = {};
extend(Endpoint.constructorParams, MediaElement.constructorParams);

/**
 * @type   module:kwsMediaApi~Endpoint.events
 * @extend module:kwsMediaApi~MediaElement.events
 */
Endpoint.events = [];
Endpoint.events.concat(MediaElement.events);


module.exports = Endpoint;
