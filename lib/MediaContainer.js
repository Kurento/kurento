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

var MediaObject = require('./MediaObject');


/**
 * Represent an instance of a MediaContainer
 *
 * This is an API abstraction not available on server side
 *
 * @abstract
 * @class
 *
 * @param {} objectRef
 * @param {*} parent
 * @param {MediaPipeline} [pipeline]
 */
function MediaContainer(objectRef, parent, pipeline)
{
  MediaObject.call(this, objectRef, parent, pipeline);


  // Bubble the event for new MediaObjects
  this.on('mediaObject', function(mediaObject)
  {
    parent.emit('mediaObject', mediaObject);
  });
};
MediaContainer.prototype.__proto__   = MediaObject.prototype;
MediaContainer.prototype.constructor = MediaContainer;


module.exports = MediaContainer;