/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

var checkParams = require('checktype').checkParams;

var createPromise = require('./createPromise');
var register = require('./register');

var Transaction = require('./TransactionsManager').Transaction;

/**
 * Get the constructor for a type
 *
 * If the type is not registered, use generic {module:core/abstracts.MediaObject}
 */
function getConstructor(type) {
  var result = register.classes[type] || register.abstracts[type];
  if (result) return result;

  console.warn("Unknown type '" + type + "', using MediaObject instead");
  return MediaObject;
};

function createConstructor(item) {
  var constructor = getConstructor(item.type);

  if (constructor.create) {
    item = constructor.create(item.params);

    // Apply inheritance
    var prototype = constructor.prototype;
    inherits(constructor, getConstructor(item.type));
    extend(constructor.prototype, prototype);
  };

  constructor.item = item;

  return constructor;
}

function MediaObjectCreator(host, encodeCreate, encodeRpc, encodeTransaction) {
  function createObject(constructor) {
    var mediaObject = new constructor()

    mediaObject.on('_rpc', encodeRpc);

    if (mediaObject instanceof register.abstracts.Hub || mediaObject instanceof register
      .classes.MediaPipeline)
      mediaObject.on('_create', encodeCreate);

    if (mediaObject instanceof register.classes.MediaPipeline)
      mediaObject.on('_transaction', encodeTransaction);

    return mediaObject;
  };

  /**
   * Request to the server to create a new MediaElement
   */
  function createMediaObject(item, callback) {
    var transaction = item.transaction;
    delete item.transaction;

    var constructor = createConstructor(item);

    item = constructor.item;
    delete constructor.item;

    if (host instanceof register.classes.MediaPipeline)
      item.params.mediaPipeline = host;

    item.constructorParams = checkParams(item.params,
      constructor.constructorParams,
      item.type);
    delete item.params;

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

    params = params || {};

    if (type instanceof Array)
      return createPromise(type, createMediaObject, callback)

    type = {
      params: params,
      transaction: transaction,
      type: type
    };

    return createMediaObject(type, callback)
  };

  this.createInmediate = function (item) {
    var constructor = createConstructor(item);
    delete constructor.item;

    return createObject(constructor);
  }
}

module.exports = MediaObjectCreator;
