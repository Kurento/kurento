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

var EventEmitter = require('events').EventEmitter;

var inherits = require('inherits');

var noop = require('../utils').noop;


/**
 * Media API for the Kurento Web SDK
 *
 * @module kwsMediaApi
 *
 * @copyright 2013-2014 Kurento (http://kurento.org/)
 * @license LGPL
 */


/**
 * Base for all objects that can be created in the media server.
 *
 * @abstract
 * @class   module:kwsMediaApi~MediaObject
 * @extends external:EventEmitter
 */

/**
 *
 * @constructor
 *
 * @param {string} id
 */
function MediaObject(id)
{
  var self = this;

  EventEmitter.call(this);


  //
  // Define object properties
  //

  /**
   * Unique identifier of this object
   *
   * @public
   * @readonly
   * @member {external:Number}
   */
  Object.defineProperty(this, "id", {value: id});


  //
  // Subscribe and unsubscribe events on the server when adding and removing
  // event listeners on this MediaObject
  //

  var subscriptions = {};

  this.on('removeListener', function(event, listener)
  {
    // Blacklisted events
    if(event == 'release'
    || event == '_rpc'
    || event == 'newListener')
      return;

    var count = EventEmitter.listenerCount(self, event);
    if(count) return;

    var token = subscriptions[event];

    this.emit('_rpc', 'unsubscribe', {subscription: token}, function(error)
    {
      if(error) return self.emit('error', error);

      delete subscriptions[event];
    });
  });

  this.on('newListener', function(event, listener)
  {
    // Blacklisted events
    if(event == 'release'
    || event == '_rpc'
    || event == '_create')
      return;

    var count = EventEmitter.listenerCount(self, event);
    if(count) return;

    this.emit('_rpc', 'subscribe', {type: event}, function(error, token)
    {
      if(error) return self.emit('error', error);

      subscriptions[event] = token;
    });
  });
};
inherits(MediaObject, EventEmitter);


/**
 * Send a command to a media object
 *
 * @param {external:String} method - Command to be executed by the server
 * @param {module:kwsMediaApi~MediaObject.constructorParams} [params]
 * @callback {createMediaObjectCallback} callback
 *
 * @return {module:kwsMediaApi~MediaObject} The own media object
 */
MediaObject.prototype.invoke = function(method, params, callback)
{
  // Fix optional parameters
  if(params instanceof Function)
  {
    if(callback)
      throw new SyntaxError("Nothing can be defined after the callback");

    callback = params;
    params = undefined;
  };

  // Generate request parameters
  var params2 =
  {
    operation: method
  };

  if(params)
    params2.operationParams = params;

  var callback2 = undefined;
  if(callback)
    callback2 = function(error, result)
    {
      if(error) return callback(error);

      callback(null, result.value);
    };

  // Do request
  this.emit('_rpc', 'invoke', params2, callback2);
};

/**
 * Explicity release a {@link module:kwsMediaApi~MediaObject MediaObject} from memory
 *
 * All its descendants will be also released and collected
 *
 * @throws {module:kwsMediaApi~MediaServerError}
 */
MediaObject.prototype.release = function(callback){
  var self = this;

  callback = callback || noop;

  this.emit('_rpc', 'release', {}, function(error)
  {
    if(error) return callback(error);

    self.emit('release');

    // Remove events on the object and remove object from cache
    self.removeAllListeners();

    callback();
  });
};


/**
 *
 *
 * @callback createMediaObjectCallback
 * @param {MediaServerError} error
 * @param {module:kwsMediaApi~MediaObject} mediaObject - The created media object child instance
 */

/**
 * @type   module:kwsMediaApi~MediaObject.constructorParams
 */
MediaObject.constructorParams =
{
  /**
   * @type Boolean
   */
  collectOnUnreferenced:
  {
    type: 'boolean'
  },

  /**
   * @type integer
   */
  garbageCollectorPeriod:
  {
    type: 'integer'
  }
};

/**
 * @type   module:kwsMediaApi~MediaObject.events
 */
MediaObject.events = ['Error'];


module.exports = MediaObject;
