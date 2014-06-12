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

var MediaObject = require('./MediaObject');


/**
 * A Hub is a routing :rom:cls:`MediaObject`. It connects several :rom:cls:`endpoints <Endpoint>` together
 *
 * @abstract
 * @class   module:kwsMediaApi/core~Hub
 * @extends module:kwsMediaApi~MediaObject
 */

/**
 * @constructor
 *
 * @param {string} id
 */
function Hub(id)
{
  MediaObject.call(this, id);
};
inherits(Hub, MediaObject);


/**
 * Create a new instance of a {module:kwsMediaApi/core~HubPort} attached to this {module:kwsMediaApi/core~Hub}
 *
 * @callback {createHubCallback} callback
 *
 * @return {module:kwsMediaApi/core~Hub} The hub itself
 */
Hub.prototype.createHubPort = function(callback)
{
  var self = this;

  var promise = new Promise(function(resolve, reject)
  {
    self.emit('_create', 'HubPort', {hub: self}, function(error, result)
    {
      if(error) return reject(error);

      resolve(result);
    });
  });

  if(callback)
    promise.then(function(result)
    {
      callback(null, result);
    },
    function(error)
    {
      callback(error);
    });

  return promise;
};
/**
 * @callback module:kwsMediaApi/core~Hub~createHubCallback
 * @param {Error} error
 * @param {module:kwsMediaApi/core~HubPort} result
 *  The created HubPort
 */


/**
 * @type module:kwsMediaApi/core~Hub.constructorParams
 */
Hub.constructorParams = {};

/**
 * @type   module:kwsMediaApi/core~Hub.events
 * @extend module:kwsMediaApi~MediaObject.events
 */
Hub.events = [];
Hub.events.concat(MediaObject.events);


module.exports = Hub;


Hub.check = function(key, value)
{
  if(!(value instanceof Hub))
    throw SyntaxError(key+' param should be a Hub, not '+typeof value);
};
