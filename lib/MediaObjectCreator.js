/*
 * (C) Copyright 2014-2015 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

var async = require('async');

var checkType = require('./checkType');
var checkParams = checkType.checkParams;
var extend = require('extend');

var createPromise = require('./createPromise');
var register = require('./register');

var Transaction = require('./TransactionsManager').Transaction;

/**
 * Get the constructor for a type
 *
 * If the type is not registered, use generic {module:core/abstracts.MediaObject}
 *
 * @function module:kurentoClient~MediaObjectCreator~getConstructor
 *
 * @param {external:string} type
 * @param {external:Boolean} strict
 *
 * @return {module:core/abstracts.MediaObject}
 */
function getConstructor(type, strict) {
  var result = register.classes[type.qualifiedType] || register.abstracts[type.qualifiedType] ||
    register.classes[type.type] || register.abstracts[type.type] ||
    register.classes[type] || register.abstracts[type];
  if (result) return result;

  if (type.hierarchy != undefined) {
    for (var i = 0; i <= type.hierarchy.length - 1; i++) {
      var result = register.classes[type.hierarchy[i]] || register.abstracts[
        type.hierarchy[i]];
      if (result) return result;
    };
  }
  if (strict) {
    var error = new SyntaxError("Unknown type '" + type + "'")
    error.type = type

    throw error
  }

  console.warn("Unknown type '", type, "', using MediaObject instead");
  return register.abstracts.MediaObject;
};

/**
 * @function module:kurentoClient~MediaObjectCreator~createConstructor
 *
 * @param item
 * @param {external:Boolean} strict
 *
 * @return {module:core/abstracts.MediaObject}
 */
function createConstructor(item, strict) {
  var constructor = getConstructor(item, strict);

  if (constructor.create) {
    item = constructor.create(item.params);

    // Apply inheritance
    var prototype = constructor.prototype;
    inherits(constructor, getConstructor(item, strict));
    extend(constructor.prototype, prototype);
  };

  constructor.item = item;

  return constructor;
}

var checkMediaElement = checkType.bind(null, 'MediaElement', 'media');

/**
 * @class module:kurentoClient~MediaObjectCreator
 *
 * @param host
 * @param encodeCreate
 * @param encodeRpc
 * @param encodeTransaction
 * @param describe
 * @param-[strict]
 */
function MediaObjectCreator(host, encodeCreate, encodeRpc, encodeTransaction,
  describe, strict) {
  if (!(this instanceof MediaObjectCreator))
    return new MediaObjectCreator(host, encodeCreate, encodeRpc,
      encodeTransaction, describe)

  /**
   * @param constructor
   *
   * @return {module:core/abstracts.MediaObject}
   */
  function createObject(constructor) {
    var mediaObject = new constructor(strict)

    mediaObject.on('_describe', describe);
    mediaObject.on('_rpc', encodeRpc);

    if (mediaObject instanceof register.abstracts['kurento.Hub'] || mediaObject instanceof register
      .classes['kurento.MediaPipeline'])
      mediaObject.on('_create', encodeCreate);

    if (mediaObject instanceof register.classes['kurento.MediaPipeline'])
      mediaObject.on('_transaction', encodeTransaction);

    return mediaObject;
  };

  /**
   * Request to the server to create a new MediaElement
   *
   * @param item
   * @param {module:kurentoClient~MediaObjectCreator~createMediaObjectCallback} [callback]
   */
  function createMediaObject(item, callback) {
    var transaction = item.transaction;
    delete item.transaction;

    var constructor = createConstructor(item, strict);

    item = constructor.item;
    delete constructor.item;

    var params = item.params || {};
    delete item.params;

    if (params.mediaPipeline == undefined && host instanceof register.classes.MediaPipeline)
      params.mediaPipeline = host;

    var params_ = extend({}, params)
    item.constructorParams = checkParams(params_, constructor.constructorParams,
      item.type);

    if (Object.keys(params_)) {
      item.properties = params_;
    }

    if (!Object.keys(item.constructorParams).length)
      delete item.constructorParams;

    try {
      var mediaObject = createObject(constructor)
    } catch (error) {
      return callback(error)
    };

    Object.defineProperty(item, 'object', {
      value: mediaObject
    });

    encodeCreate(transaction, item, callback);

    return mediaObject
  };
  /**
   * @callback module:kurentoClient~MediaObjectCreator~createMediaObjectCallback
   * @param {external:Error} error
   */

  /**
   * @method module:kurentoClient~MediaObjectCreator#create
   *
   * @param type
   * @param params
   * @param {module:kurentoClient~MediaObjectCreator~createCallback} [callback]
   */
  this.create = function (type, params, callback) {
    var transaction = (arguments[0] instanceof Transaction) ? Array.prototype
      .shift.apply(arguments) : undefined;

    switch (arguments.length) {
    case 1:
      params = undefined;
    case 2:
      callback = undefined;
    };

    // Fix optional parameters
    if (params instanceof Function) {
      if (callback)
        throw new SyntaxError("Nothing can be defined after the callback");

      callback = params;
      params = undefined;
    };

    if (type instanceof Array) {
      var createPipeline = false

      type.forEach(function (request) {
        var params = request.params || {}

        if (typeof params.mediaPipeline === 'number')
          createPipeline = true
      })

      function connectElements(error, elements) {
        if (error) return callback(error)

        if (params === true && host.connect)
          return host.connect(elements.filter(function (element) {
              try {
                checkMediaElement(element)
                return true
              } catch (e) {}
            }),
            function (error) {
              if (error) return callback(error)

              callback(null, elements)
            })

        callback(null, elements)
      }

      if (createPipeline)
        return host.transaction(function () {
          var mediaObjects = []

          async.map(type, function (request, callback) {
              var params = request.params || {}

              if (typeof params.mediaPipeline === 'number')
                params.mediaPipeline = mediaObjects[params.mediaPipeline]

              mediaObjects.push(createMediaObject(request, callback))
            },
            connectElements)
        })

      return createPromise(type, createMediaObject, connectElements)
    }

    type = {
      params: params,
      transaction: transaction,
      type: type
    };

    return createMediaObject(type, callback)
  };
  /**
   * @callback module:kurentoClient~MediaObjectCreator~createCallback
   *
   * @param {external:Error} error
   * @param {module:core/abstracts.MediaObject} mediaObject
   *  The created MediaObject
   */

  /**
   * @method module:kurentoClient~MediaObjectCreator#createInmediate
   *
   * @param item
   */
  this.createInmediate = function (item) {
    var constructor = createConstructor(item, strict);
    delete constructor.item;

    return createObject(constructor);
  }
}

module.exports = MediaObjectCreator;
