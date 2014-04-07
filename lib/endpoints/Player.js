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

/**
 * Media API for the Kurento Web SDK
 *
 * @module kwsMediaApi/endpoints
 *
 * @copyright 2014 Kurento (http://kurento.org/)
 * @license LGPL
 */

var Uri = require('./Uri');

var extend   = require('extend');
var inherits = require('inherits');


/**
 * @class
 * @extends module:kwsMediaApi/endpoints~Player
 *
 * @param id
 * @param {module:kwsMediaApi~MediaContainer} parent
 * @param {module:kwsMediaApi~MediaPipeline} pipeline
 * @param {module:kwsMediaApi/endpoints~Uri.ConstructorParams} params
 */
function Player(id, parent, pipeline, params)
{
	Uri.call(this, id, parent, pipeline, params);
};
inherits(Player, Uri);


Player.prototype.play = function(callback)
{
  return this.invoke('play', callback);
};


/**
 * @see {@link module:kwsMediaApi/endpoints~Uri.paramsScheme}
 */
Player.paramsScheme = {
  mediaPipeline: {
    type: 'MediaPipeline',
    required: true
  },

  uri:
  {
    type: 'String',
    required: true
  },

  useEncodedMedia: {
    type: 'boolean',
    required: false
  }
}
extend(Player.paramsScheme, Uri.paramsScheme);


Player.create = function(pipeline, params, callback)
{
  pipeline.createMediaElement('PlayerEndpoint', params, callback);
};


module.exports = Player;
