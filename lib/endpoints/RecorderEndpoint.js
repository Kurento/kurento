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
 * @module kwsMediaApi/endpoints
 *
 * @copyright 2013-2014 Kurento (http://kurento.org/)
 * @license LGPL
 */

var UriEndpoint = require('./UriEndpoint');


/**
 * Provides function to store contents in reliable mode (doesn't discard data). It contains :rom:cls:`MediaSink` pads for audio and video.
 *
 * @class   module:kwsMediaApi/endpoints~RecorderEndpoint
 * @extends module:kwsMediaApi~UriEndpoint
 */

/**
 * 
 *
 * @constructor
 *
 * @param {string} id
 */
function RecorderEndpoint(id)
{
  UriEndpoint.call(this, id);
};
inherits(RecorderEndpoint, UriEndpoint);


/**
 * Starts storing media received through the :rom:cls:`MediaSink` pad
 *
 * @param {module:kwsMediaApi/endpoints~RecorderEndpoint.recordCallback} [callback]
 *
 * @return {module:kwsMediaApi/endpoints~RecorderEndpoint}
 *  The own media object
 */
RecorderEndpoint.prototype.record = function(callback){
  return this.invoke('record', callback);
};
/**
 * @callback RecorderEndpoint~recordCallback
 * @param {Error} error
 */


/**
 * @type module:kwsMediaApi/endpoints~RecorderEndpoint.constructorParams
 *
 * @property {MediaPipeline} mediaPipeline
 *  the :rom:cls:`MediaPipeline` to which the endpoint belongs
 *
 * @property {MediaProfileSpecType} [mediaProfile]
 *  Choose either a :rom:attr:`MediaProfileSpecType.WEBM` or a :rom:attr:`MediaProfileSpecType.MP4` profile for recording
 *
 * @property {boolean} [stopOnEndOfStream]
 *  Forces the recorder endpoint to finish processing data when an :term:`EOS` is detected in the stream
 *
 * @property {String} uri
 *  URI where the recording will be stored
 */
RecorderEndpoint.constructorParams = {
  mediaPipeline: {
    type: 'MediaPipeline',
    required: true
  },

  mediaProfile: {
    type: 'MediaProfileSpecType',
  },

  stopOnEndOfStream: {
    type: 'boolean',
  },

  uri: {
    type: 'String',
    required: true
  },
};

/**
 * @type   module:kwsMediaApi/endpoints~RecorderEndpoint.events
 * @extend module:kwsMediaApi~UriEndpoint.events
 */
RecorderEndpoint.events = [];
RecorderEndpoint.events.concat(UriEndpoint.events);


module.exports = RecorderEndpoint;


RecorderEndpoint.check = function(key, value)
{
  if(!(value instanceof RecorderEndpoint))
    throw SyntaxError(key+' param should be a RecorderEndpoint, not '+typeof value);
};
