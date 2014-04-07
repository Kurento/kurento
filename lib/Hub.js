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

var MediaContainer = require('./MediaContainer');


/**
 * A Hub is a routing :rom:cls:`MediaObject`. It connects several :rom:cls:`endpoints <Endpoint>` together
 *
 * @abstract
 * @class   module:kwsMediaApi~Hub
 * @extends module:kwsMediaApi~MediaContainer
 */

/**
 *
 * @constructor
 *
 * @param {string} id
 * @param {module:kwsMediaApi~MediaContainer} parent
 * @param {module:kwsMediaApi~MediaPipeline} pipeline
 * @param {module:kwsMediaApi~Hub.constructorParams} params
 */
function Hub(id, parent, pipeline, params)
{
  MediaContainer.call(this, id, parent, pipeline, params);
};
inherits(Hub, MediaContainer);


/**
 * @type   module:kwsMediaApi~Hub.constructorParams
 * @extend module:kwsMediaApi~MediaContainer.constructorParams
 */
Hub.constructorParams = {};
extend(Hub.constructorParams, MediaContainer.constructorParams);

/**
 * @type   module:kwsMediaApi~Hub.events
 * @extend module:kwsMediaApi~MediaContainer.events
 */
Hub.events = [];
Hub.events.concat(MediaContainer.events);


module.exports = Hub;
