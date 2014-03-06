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
 * @module KwsMedia/elements
 *
 * @copyright 2014 Kurento (http://kurento.org/)
 * @license LGPL
 */

var MediaElement = require('../MediaElement');

var extend   = require('extend');
var inherits = require('inherits');


/**
 * @class
 * @extends module:KwsMedia/endpoints~Uri
 *
 * @param id
 * @param {module:KwsMedia~MediaContainer} parent
 * @param {module:KwsMedia~MediaPipeline} pipeline
 * @param {module:KwsMedia/endpoints~Uri.paramsScheme} params
 */
function Uri(id, parent, pipeline, params)
{
  MediaElement.call(this, id, parent, pipeline, params);
};
inherits(Uri, MediaElement);


Uri.prototype.getUri = function(callback)
{
  return this.invoke('getUri', callback);
};

Uri.prototype.play = function(callback)
{
  return this.invoke('play', callback);
};

Uri.prototype.pause = function(callback)
{
  return this.invoke('pause', callback);
};

Uri.prototype.stop = function(callback)
{
  return this.invoke('stop', callback);
};


Uri.paramsScheme =
{
  uri:
  {
    type: 'String',
    required: true
  }
};
extend(Uri.paramsScheme, MediaElement.paramsScheme);


module.exports = Uri;
