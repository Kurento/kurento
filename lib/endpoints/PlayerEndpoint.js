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
 * Retrieves content from seekable sources in reliable
mode (does not discard media information) and inject 
them into :term:`KMS`. It
contains one :rom:cls:`MediaSource` for each media type detected.
 *
 * @class   module:kwsMediaApi/endpoints~PlayerEndpoint
 * @extends module:kwsMediaApi~UriEndpoint
 */

/**
 * Create a PlayerEndpoint
 *
 * @constructor
 *
 * @param {string} id
 */
function PlayerEndpoint(id)
{
  UriEndpoint.call(this, id);
};
inherits(PlayerEndpoint, UriEndpoint);


/**
 * Starts to send data to the endpoint :rom:cls:`MediaSource`
 *
 * @param {module:kwsMediaApi/endpoints~PlayerEndpoint.playCallback} [callback]
 *
 * @return {module:kwsMediaApi/endpoints~PlayerEndpoint}
 *  The own media object
 */
PlayerEndpoint.prototype.play = function(callback){
  this.invoke('play', callback);

  return this;
};
/**
 * @callback PlayerEndpoint~playCallback
 * @param {Error} error
 */


/**
 * @type module:kwsMediaApi/endpoints~PlayerEndpoint.constructorParams
 *
 * @property {MediaPipeline} mediaPipeline
 *  The :rom:cls:`MediaPipeline` this PlayerEndpoint belongs to.
 *
 * @property {String} uri
 *  URI that will be played
 *
 * @property {boolean} [useEncodedMedia]
 *  use encoded instead of raw media. If the parameter is false then the
element uses raw media. Changing this parameter can affect stability
severely, as lost key frames lost will not be regenerated. Changing the media type does not
affect to the result except in the performance (just in the case where
original media and target media are the same) and in the problem with the
key frames. We strongly recommended not to use this parameter because
correct behaviour is not guarantied.
 */
PlayerEndpoint.constructorParams = {
  mediaPipeline: {
    type: 'MediaPipeline',
    required: true
  },

  uri: {
    type: 'String',
    required: true
  },

  useEncodedMedia: {
    type: 'boolean',
  },
};

/**
 * @type   module:kwsMediaApi/endpoints~PlayerEndpoint.events
 * @extend module:kwsMediaApi~UriEndpoint.events
 */
PlayerEndpoint.events = ['EndOfStream'];
PlayerEndpoint.events.concat(UriEndpoint.events);


module.exports = PlayerEndpoint;


PlayerEndpoint.check = function(key, value)
{
  if(!(value instanceof PlayerEndpoint))
    throw SyntaxError(key+' param should be a PlayerEndpoint, not '+typeof value);
};
