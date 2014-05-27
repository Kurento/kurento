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

var Promise = require('es6-promise').Promise;

var noop = require('./utils').noop;


/**
 * Media API for the Kurento Web SDK
 *
 * @module kwsMediaApi
 *
 * @copyright 2013-2014 Kurento (http://kurento.org/)
 * @license LGPL
 */


/**
 * Internal base class for all objects that can be created in the media server.
 *
 * @abstract
 * @class   module:kwsMediaApi~_MediaObject
 * @extends external:EventEmitter
 */

/**
 *
 * @constructor
 *
 * @param {string} id
 */
function _MediaObject(id)
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
inherits(_MediaObject, EventEmitter);


/**
 * Send a command to a media object
 *
 * @param {external:String} method - Command to be executed by the server
 * @param {module:kwsMediaApi~_MediaObject.constructorParams} [params]
 * @callback {invokeCallback} callback
 *
 * @return {module:kwsMediaApi~_MediaObject} The own media object
 */
_MediaObject.prototype.invoke = function(method, params, callback){
  var self = this;

  // Fix optional parameters
  if(params instanceof Function)
  {
    if(callback)
      throw new SyntaxError("Nothing can be defined after the callback");

    callback = params;
    params = undefined;
  };

  var promise = new Promise(function(resolve, reject)
  {
    // Generate request parameters
    var params2 =
    {
      operation: method
    };

    if(params)
      params2.operationParams = params;

    // Do request
    self.emit('_rpc', 'invoke', params2, function(error, result)
    {
      if(error) return reject(error);

      resolve(result.value);
    });
  });

  if(callback)
    promise.then(function(result)
    {
      callback(null, result);
    },
    callback);

  return promise;
};
/**
 * @callback invokeCallback
 * @param {MediaServerError} error
 */

/**
 * Explicity release a {@link module:kwsMediaApi~_MediaObject MediaObject} from memory
 *
 * All its descendants will be also released and collected
 *
 * @throws {module:kwsMediaApi~MediaServerError}
 */
_MediaObject.prototype.release = function(callback){
  var self = this;

  var promise = new Promise(function(resolve, reject)
  {
    self.emit('_rpc', 'release', {}, function(error)
    {
      if(error) return reject(error);

      self.emit('release');

      // Remove events on the object and remove object from cache
      self.removeAllListeners();

      resolve();
    });
  });

  if(callback)
    promise.then(function()
    {
      callback();
    },
    callback);

  return promise;
};


module.exports = _MediaObject;
