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
 * @module kwsMediaApi/hubs
 *
 * @copyright 2013-2014 Kurento (http://kurento.org/)
 * @license LGPL
 */

var Hub = require('../core/Hub');


/**
 * A :rom:cls:`Hub` that sends a given source to all the connected sinks
 *
 * @class   module:kwsMediaApi/hubs~DispatcherOneToMany
 * @extends module:kwsMediaApi~Hub
 */

/**
 * Create a :rom:cls:`DispatcherOneToMany` belonging to the given pipeline.
 *
 * @constructor
 *
 * @param {string} id
 */
function DispatcherOneToMany(id)
{
  Hub.call(this, id);
};
inherits(DispatcherOneToMany, Hub);


/**
 * Remove the source port and stop the media pipeline.
 *
 * @param {module:kwsMediaApi/hubs~DispatcherOneToMany.removeSourceCallback} [callback]
 *
 * @return {module:kwsMediaApi/hubs~DispatcherOneToMany}
 *  The own media object
 */
DispatcherOneToMany.prototype.removeSource = function(callback){
  return this.invoke('removeSource', callback);
};
/**
 * @callback DispatcherOneToMany~removeSourceCallback
 * @param {Error} error
 */

/**
 * Sets the source port that will be connected to the sinks of every :rom:cls:`HubPort` of the dispatcher
 *
 * @param {HubPort} source
 *  source to be broadcasted
 *
 * @param {module:kwsMediaApi/hubs~DispatcherOneToMany.setSourceCallback} [callback]
 *
 * @return {module:kwsMediaApi/hubs~DispatcherOneToMany}
 *  The own media object
 */
DispatcherOneToMany.prototype.setSource = function(source, callback){
  checkType('HubPort', 'source', source, {required: true});

  var params = {
    source: source,
  };

  return this.invoke('setSource', params, callback);
};
/**
 * @callback DispatcherOneToMany~setSourceCallback
 * @param {Error} error
 */


/**
 * @type module:kwsMediaApi/hubs~DispatcherOneToMany.constructorParams
 *
 * @property {MediaPipeline} mediaPipeline
 *  the :rom:cls:`MediaPipeline` to which the dispatcher belongs
 */
DispatcherOneToMany.constructorParams = {
  mediaPipeline: {
    type: 'MediaPipeline',
    required: true
  },
};

/**
 * @type   module:kwsMediaApi/hubs~DispatcherOneToMany.events
 * @extend module:kwsMediaApi~Hub.events
 */
DispatcherOneToMany.events = [];
DispatcherOneToMany.events.concat(Hub.events);


module.exports = DispatcherOneToMany;


DispatcherOneToMany.check = function(key, value)
{
  if(!(value instanceof DispatcherOneToMany))
    throw SyntaxError(key+' param should be a DispatcherOneToMany, not '+typeof value);
};
