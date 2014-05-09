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
 * @module kwsMediaApi/hubs
 *
 * @copyright 2013-2014 Kurento (http://kurento.org/)
 * @license LGPL
 */

var Hub = require('../core/Hub');


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
 */
function Composite(id)
{
  Hub.call(this, id);
};
inherits(Composite, Hub);


/**
 * @type module:kwsMediaApi/hubs~Composite.constructorParams
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

/**
 * @type   module:kwsMediaApi/hubs~Composite.events
 * @extend module:kwsMediaApi~Hub.events
 */
Composite.events = [];
Composite.events.concat(Hub.events);


module.exports = Composite;


Composite.check = function(key, value)
{
  if(!(value instanceof Composite))
    throw SyntaxError(key+' param should be a Composite, not '+typeof value);
};
