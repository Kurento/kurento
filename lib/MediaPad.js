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

var MediaObject = require('MediaObject');


/**
 *
 *
 * @private
 * @abstract
 * @class
 * @extends {MediaObject}
 *
 * @param {MediaElement} mediaElement - Element owner of this pad
 * @param {MediaPad.MediaType} type - Type of this pad
 * @param {string} description - Description of this pad
 */
function MediaPad(objectRef, parent, pipeline, mediaElement, description)
{
  MediaObject.call(this, objectRef, parent, pipeline);

  /**
   * {MediaElement} that owns this {MediaPad}
   *
   * @public
   * @readonly
   * @member {MediaElement} mediaElement
   */
  Object.defineProperty(this, "mediaElement", {value : mediaElement});

  /**
   * Type of this {MediaPad}
   *
   * @public
   * @readonly
   * @member {MediaPad.MediaType} type
   */
  Object.defineProperty(this, "type", {value : objectRef.type});

  /**
   * Description string of this {MediaPad}
   *
   * @public
   * @readonly
   * @member {string} description
   */
  Object.defineProperty(this, "description", {value : description});
};
MediaPad.prototype.__proto__   = MediaObject.prototype;
MediaPad.prototype.constructor = MediaPad;


/**
 *
 *
 * @private
 * @class
 * @extends {MediaObject}
 *
 * @param {MediaElement} mediaElement - Element owner of this pad
 * @param {MediaPad.MediaType} type - Type of this pad
 * @param {string} description - Description of this pad
 */
function MediaPadSource(objectRef, parent, pipeline, mediaElement, description)
{
  MediaPad.call(this, objectRef, parent, pipeline, mediaElement, description);


  var sinks = [];


  /**
   * Connect this source pad to the specified sink pad
   *
   * @public
   *
   * @param {...MediaPadSink} sink - Sink pad to be connected
   *
   * @return {MediaPadSource} The own pad
   *
   * @throws {MediaServerError}
   */
  this.connect = function(sink, callback)
  {
    var params =
    {
      sink:
      {
        id:    sink.id,
        token: sink.token
      }
    };

    this._rpc('connect', params, function(error)
    {
      if(error)
      {
        if(callback)
           callback(error);

        return;
      };

      Object.defineProperty(sink, 'source', {value: this});
    });

    return this;
  };

  /**
   * Disconnect this source pad from the specified sink pad
   *
   * @public
   *
   * @param {...MediaPadSink} sink - Sink pad to be disconnected
   *
   * @return {MediaPadSource} The own pad
   *
   * @throws {MediaServerError}
   */
  this.disconnect = function(sink, callback)
  {
    var params =
    {
      sink:
      {
        id:    sink.id,
        token: sink.token
      }
    };

    this._rpc('disconnect', params, function(error)
    {
      if(error)
      {
        if(callback)
           callback(error);

        return;
      };

      Object.defineProperty(sink, 'source', {value: undefined});
    });

    return this;
  };

  /**
   * List of sink pads currently connected to this source pad
   *
   * @public
   * @readonly
   * @member {MediaPadSink[]} connectedSinks
   *
   * @throws {MediaServerException}
   */
  this.__defineGetter__('sinks', function()
  {
    return sinks;
  });
}
MediaPadSource.prototype.__proto__   = MediaPad.prototype;
MediaPadSource.prototype.constructor = MediaPadSource;


/**
 *
 *
 * @private
 * @class
 * @extends {MediaObject}
 *
 * @param {MediaElement} mediaElement - Element owner of this pad
 * @param {MediaPad.MediaType} type - Type of this pad
 * @param {string} description - Description of this pad
 */
function MediaPadSink(objectRef, parent, pipeline, mediaElement, description)
{
  MediaPad.call(this, objectRef, parent, pipeline, mediaElement, description);


  /**
   * Source pad currently connected to this sink pad
   *
   * @public
   * @readonly
   * @member {MediaPadSource} source
   */
  Object.defineProperty(this, 'source', {enumerable: true});
}
MediaPadSink.prototype.__proto__   = MediaPad.prototype;
MediaPadSink.prototype.constructor = MediaPadSink;


/**
 *
 *
 * @typedef {(audio|video|data)} MediaPad.MediaType
 */


exports.MediaPadSink   = MediaPadSink;
exports.MediaPadSource = MediaPadSource;