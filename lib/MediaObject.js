/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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


/**
 *
 *
 * @module
 *
 * @copyright 2013 Kurento (http://kurento.org/)
 * @license LGPL
 */

var EventEmitter = require('events').EventEmitter;


/**
 * Represent an instance of a server-side MediaObject
 *
 * @abstract
 * @class
 *
 * @param {} objectRef
 * @param {*} parent
 * @param {MediaPipeline} [pipeline]
 */
function MediaObject(objectRef, parent, pipeline)
{
  var self = this;

  EventEmitter.call(this);


  //
  // Subscribe and unsubscribe events on the server when adding and removing
  // event listeners on this MediaObject
  //

  var tokens = {};

  this.on('newListener', function(event, listener)
  {
    var count = EventEmitter.listenerCount(self, event);

    if(count == 1)
    {
      var params =
      {
        type: event
      };

      this._rpc('subscribeEvent', params, function(error, token)
      {
        if(error)
        {
          listener(error);
          return;
        };

        tokens[event] = token;
      });
    };
  });

  this.on('removeListener', function(event, listener)
  {
    var count = EventEmitter.listenerCount(self, event);

    if(count < 1)
    {
      var token = tokens[event];

      var params =
      {
        token: token
      };

      this._rpc('unsubscribeEvent', params, function(error)
      {
        if(error)
        {
          listener(error);
          return;
        };

        delete tokens[event];
      });
    };
  });


  //
  // Define object properties
  //

  /**
   * Unique identifier of this object
   *
   * @public
   * @readonly
   * @member {number}
   */
  Object.defineProperty(this, "id", {value : objectRef.id});

  /**
   *
   *
   * @public
   * @readonly
   * @member {string}
   */
  Object.defineProperty(this, "token", {value : objectRef.token});

  /**
   * Parent (object that created it) of a MediaObject
   *
   * @public
   * @readonly
   * @member {MediaObject}
   */
  Object.defineProperty(this, "parent", {value : parent});

  /**
   * Pipeline to which this MediaObjects belong
   *
   * If this MediaObject is a pipeline, return itself
   *
   * @public
   * @readonly
   * @member {MediaPipeline}
   */
  Object.defineProperty(this, "pipeline", {value : pipeline || this});


  // Notify that this MediaObject has been created
  parent.emit('mediaObject', this);
};
MediaObject.prototype.__proto__   = EventEmitter.prototype;
MediaObject.prototype.constructor = MediaObject;

/**
 * 
 * @private
 * 
 * @param method
 * @param params
 * @param callback
 */
MediaObject.prototype._rpc = function(method, params, callback)
{
  var self = this;

  params.object =
  {
    id:    this.id,
    token: this.token
  };

  if(callback)
     callback = function(error, result)
     {
       callback(error, result);

       // Dispatch an event with the called method
       self.emit(method);
     };

  this.emit('_rpc', method, params, callback);
};

/**
 * Explicity release a MediaObject from memory
 *
 * All its descendants will be also released and collected
 *
 * @throws {MediaServerError}
 */
MediaObject.prototype.release = function()
{
  var self = this;

  this._rpc('release', {}, function(error)
  {
    if(error)
    {
      console.error(error);
      return;
    };

    // Remove events on the object and remove object from cache
    self.removeAllListeners();
  });

  return this;
};

/**
 * Send a comand to a media object
 *
 * @param {string} method - Command to be executed by the server
 * @param {Object} [params] -
 * @callback {createMediaObjectCallback} callback
 *
 * @return {MediaObject} The own media object
 */
MediaObject.prototype.invoke = function(method, params, callback)
{
  // Fix optional parameters
  if(params instanceof Function)
  {
    if(callback)
      throw new SyntaxError("Nothing can be defined after the callback");

    callback = params;
    params = null;
  };

  // Generate request parameters
  var params2 =
  {
    method: method
  };

  if(params)
    params2.params = params;

  // Do request
  this._rpc('invoke', params2, callback);

  return this;
};


/**
 * 
 * @param {MediaObjectConstructorParams} params
 *
 * @throws {SyntaxError}
 *
 * @returns {MediaObjectConstructorParams}
 */
MediaObject.checkparams = function(params)
{
  /**
   * @type {MediaObjectConstructorParams}
   */
  var result = {};

  // check MediaObject params
  for(var key in params)
  {
    var value = params[key];

    switch(key)
    {
      case 'excludeFromGC':
        if(typeof value != "boolean")
          throw SyntaxError("excludeFromGC param should be Boolean");
      break;

      case 'garbageCollectorPeriod':
        if(typeof value != "number")
          throw SyntaxError("garbageCollectorPeriod param should be Integer");
      break;

      default:
        continue;
    };

    result[key] = value;
  };

  return result;
};


/**
 * @type MediaObjectConstructorParams
 *
 * @member {Boolean} excludeFromGC
 * @member {Number} garbageCollectorPeriod
 */

/**
 *
 *
 * @callback createMediaObjectCallback
 * @param {MediaServerError} error
 * @param {MediaObject} mediaObject - The created media object child instance
 */


module.exports = MediaObject;