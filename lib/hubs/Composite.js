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
 * @module kwsMediaApi/hubs
 *
 * @copyright 2013-2014 Kurento (http://kurento.org/)
 * @license LGPL
 */

var Hub = require('../Hub');


/**
 * A :rom:cls:`Hub` that mixes the :rom:attr:`MediaType.AUDIO` stream of its connected sources and constructs a grid with the :rom:attr:`MediaType.VIDEO` streams of its connected sources into its sink
 *
 * @class   module:kwsMediaApi/hubs~Composite
 * @extends module:kwsMediaApi~Hub
 */

/**
 * Create for the given pipeline
 *
 * @constructor
 *
 * @param {string} id
 * @param {module:kwsMediaApi~MediaContainer} parent
 * @param {module:kwsMediaApi~MediaPipeline} pipeline
 * @param {module:kwsMediaApi/hubs~Composite.constructorParams} params
 */
function Composite(id, parent, pipeline, params)
{
  Hub.call(this, id, parent, pipeline, params);
};
inherits(Composite, Hub);


/**
 * @type   module:kwsMediaApi/hubs~Composite.constructorParams
 * @extend module:kwsMediaApi~Hub.constructorParams
 *
 * @property {MediaPipeline} mediaPipeline
 *  the :rom:cls:`MediaPipeline` to which the dispatcher belongs
 */
Composite.constructorParams = {
  mediaPipeline: {
    type: 'MediaPipeline',
    required: true
  },
};
extend(Composite.constructorParams, Hub.constructorParams);

/**
 * @type   module:kwsMediaApi/hubs~Composite.events
 * @extend module:kwsMediaApi~Hub.events
 */
Composite.events = [];
Composite.events.concat(Hub.events);


/**
 *
 *
 * @param {module:kwsMediaApi/hubs~Composite.constructorParams} params
 */
Composite.create = function(pipeline, params, callback)
{
  pipeline.createMediaElement('Composite', params, callback);
};


module.exports = Composite;
