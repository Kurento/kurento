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
 * This :rom:cls:`MediaElement` specifies a connection with a :rom:cls:`Hub`
 *
 * @class   module:kwsMediaApi/core~HubPort
 * @extends module:kwsMediaApi~MediaElement
 */

/**
 * Creates a :rom:cls:`HubPort` for the given :rom:cls:`Hub`
 *
 * @constructor
 *
 * @param {string} id
 */
function HubPort(id)
{
  MediaElement.call(this, id);
};
inherits(HubPort, MediaElement);


/**
 * @type module:kwsMediaApi/core~HubPort.constructorParams
 *
 * @property {Hub} hub
 *  :rom:cls:`Hub` to which this port belongs
 */
HubPort.constructorParams = {
  hub: {
    type: 'Hub',
    required: true
  },
};

/**
 * @type   module:kwsMediaApi/core~HubPort.events
 * @extend module:kwsMediaApi~MediaElement.events
 */
HubPort.events = [];
HubPort.events.concat(MediaElement.events);


module.exports = HubPort;


HubPort.check = function(key, value)
{
  if(!(value instanceof HubPort))
    throw SyntaxError(key+' param should be a HubPort, not '+typeof value);
};
