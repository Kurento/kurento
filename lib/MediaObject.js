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
 * @param {module:kwsMediaApi~MediaContainer} parent
 * @param {module:kwsMediaApi~MediaPipeline} [pipeline]
 * @param {module:kwsMediaApi~MediaObject.constructorParams} params
 */
function MediaObject(id, parent, pipeline, params)
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
  Object.defineProperty(this, "id", {value : id});

  /**
   * Parent (object that created it) of a MediaObject
   *
   * @public
   * @readonly
   * @member {module:kwsMediaApi~MediaObject}
   */
  Object.defineProperty(this, "parent", {value : parent});

  /**
   * Pipeline to which this MediaObjects belong
   *
   * If this MediaObject is a pipeline, return itself
   *
   * @public
   * @readonly
   * @member {module:kwsMediaApi~MediaPipeline}
   */
  Object.defineProperty(this, "pipeline", {value : pipeline || this});

  /**
   * User defined parameters of this {module:kwsMediaApi~MediaObject}
   */
  for(var key in params)
    Object.defineProperty(this, key, {value: params[key], enumerable: true});


  //
  // Subscribe and unsubscribe events on the server when adding and removing
  // event listeners on this MediaObject
  //

  var tokens = {};

  this.on('removeListener', function(event, listener)
  {
    // Blacklisted events
    if(event == 'release'
    || event == '_rpc'
    || event == 'mediaObject'
    || event == 'newListener')
      return;

    var count = EventEmitter.listenerCount(self, event);

    if(!count)
    {
      var token = tokens[event];

      var params =
      {
        subscription: token
      };

      this.emit('_rpc', 'unsubscribe', params, function(error)
      {
        console.error(error);
        if(error) return self.emit('error', error);

        delete tokens[event];
      });
    };
  });

  this.on('newListener', function(event, listener)
  {
    // Blacklisted events
    if(event == 'release'
    || event == '_rpc')
      return;

    var count = EventEmitter.listenerCount(self, event);

    if(!count)
    {
      var params =
      {
        type: event
      };

      this.emit('_rpc', 'subscribe', params, function(error, token)
      {
        if(error) return self.emit('error', error);

        tokens[event] = token;
      });
    };
  });


  // Notify that this MediaObject has been created
  parent.emit('mediaObject', this);
};
inherits(MediaObject, EventEmitter);


/**
 * Send a command to a media object
 *
 * @param {external:String} method - Command to be executed by the server
 * @param {module:kwsMediaApi~MediaObject.constructorParams} [params] -
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
    params = null;
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

  callback = callback || function(error)
  {
    if(error) console.error(error);
  };

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
