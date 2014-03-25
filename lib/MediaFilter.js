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

var MediaElement = require('./MediaElement');


/**
 * Represent an instance of a MediaFilter
 *
 * This is a Javascript API abstraction not available on the Media Server API
 *
 * @abstract
 * @class   module:kwsMediaApi~MediaFilter
 * @extends module:kwsMediaApi~MediaElement
 *
 * @param id
 * @param {module:kwsMediaApi~MediaContainer} parent
 * @param {module:kwsMediaApi~MediaPipeline} [pipeline]
 * @param {module:kwsMediaApi~MediaObject.ConstructorParams} params
 */
function MediaFilter(id, parent, pipeline, params)
{
  // Bubble the event for new MediaObjects
  this.on('mediaObject', function(mediaObject)
  {
    parent.emit('mediaObject', mediaObject);
  });

  MediaElement.call(this, id, parent, pipeline, params);
};
MediaFilter.prototype.__proto__   = MediaElement.prototype;
MediaFilter.prototype.constructor = MediaFilter;


/**
 * @see {@link module:kwsMediaApi~MediaElement.paramsScheme}
 */
MediaFilter.paramsScheme = MediaElement.paramsScheme;


module.exports = MediaFilter;
