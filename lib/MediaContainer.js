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

var inherits = require('inherits');

var MediaObject = require('./MediaObject');


/**
 * Represent an instance of a MediaContainer
 *
 * This is a Javascript API abstraction not available on the Media Server API
 *
 * @abstract
 * @class   module:kwsMediaApi~MediaContainer
 * @extends module:kwsMediaApi~MediaObject
 *
 * @param {} id
 * @param {*} parent
 * @param {module:kwsMediaApi~MediaPipeline} [pipeline]
 */
function MediaContainer(id, parent, pipeline, params)
{
  // Bubble the event for new MediaObjects
  this.on('mediaObject', function(mediaObject)
  {
    parent.emit('mediaObject', mediaObject);
  });

  MediaObject.call(this, id, parent, pipeline, params);
};
inherits(MediaContainer, MediaObject);


/**
 * @see {@link module:kwsMediaApi~MediaObject.constructorParams}
 */
MediaContainer.constructorParams = MediaObject.constructorParams;


module.exports = MediaContainer;
