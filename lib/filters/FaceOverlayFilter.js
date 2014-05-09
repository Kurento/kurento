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
 * @module kwsMediaApi/filters
 *
 * @copyright 2013-2014 Kurento (http://kurento.org/)
 * @license LGPL
 */

var Filter = require('../core/Filter');


/**
 * FaceOverlayFilter interface. This type of :rom:cls:`Filter` detects faces in a video feed. The face is then overlaid with an image.
 *
 * @class   module:kwsMediaApi/filters~FaceOverlayFilter
 * @extends module:kwsMediaApi~Filter
 */

/**
 * FaceOverlayFilter interface. This type of :rom:cls:`Filter` detects faces in a video feed. The face is then overlaid with an image.
 *
 * @constructor
 *
 * @param {string} id
 */
function FaceOverlayFilter(id)
{
  Filter.call(this, id);
};
inherits(FaceOverlayFilter, Filter);


/**
 * Sets the image to use as overlay on the detected faces.
 *
 * @param {String} uri
 *  URI where the image is located
 *
 * @param {float} offsetXPercent
 *  the offset applied to the image, from the X coordinate of the detected face upper right corner. A positive value indicates right displacement, while a negative value moves the overlaid image to the left. This offset is specified as a percentage of the face width.

For example, to cover the detected face with the overlaid image, the parameter has to be ``0.0``. Values of ``1.0`` or ``-1.0`` indicate that the image upper right corner will be at the face´s X coord, +- the face´s width.

.. note::

    The parameter name is misleading, the value is not a percent but a ratio
 *
 * @param {float} offsetYPercent
 *  the offset applied to the image, from the Y coordinate of the detected face upper right corner. A positive value indicates up displacement, while a negative value moves the overlaid image down. This offset is specified as a percentage of the face width.

For example, to cover the detected face with the overlaid image, the parameter has to be ``0.0``. Values of ``1.0`` or ``-1.0`` indicate that the image upper right corner will be at the face´s Y coord, +- the face´s width.

.. note::

    The parameter name is misleading, the value is not a percent but a ratio
 *
 * @param {float} widthPercent
 *  proportional width of the overlaid image, relative to the width of the detected face. A value of 1.0 implies that the overlaid image will have the same width as the detected face. Values greater than 1.0 are allowed, while negative values are forbidden.

.. note::

    The parameter name is misleading, the value is not a percent but a ratio
 *
 * @param {float} heightPercent
 *  proportional height of the overlaid image, relative to the height of the detected face. A value of 1.0 implies that the overlaid image will have the same height as the detected face. Values greater than 1.0 are allowed, while negative values are forbidden.

.. note::

    The parameter name is misleading, the value is not a percent but a ratio
 *
 * @param {module:kwsMediaApi/filters~FaceOverlayFilter.setOverlayedImageCallback} [callback]
 *
 * @return {module:kwsMediaApi/filters~FaceOverlayFilter}
 *  The own media object
 */
FaceOverlayFilter.prototype.setOverlayedImage = function(uri, offsetXPercent, offsetYPercent, widthPercent, heightPercent, callback){
  checkType('String', 'uri', uri, {required: true});
  checkType('float', 'offsetXPercent', offsetXPercent, {required: true});
  checkType('float', 'offsetYPercent', offsetYPercent, {required: true});
  checkType('float', 'widthPercent', widthPercent, {required: true});
  checkType('float', 'heightPercent', heightPercent, {required: true});

  var params = {
    uri: uri,
    offsetXPercent: offsetXPercent,
    offsetYPercent: offsetYPercent,
    widthPercent: widthPercent,
    heightPercent: heightPercent,
  };

  this.invoke('setOverlayedImage', params, callback);

  return this;
};
/**
 * @callback FaceOverlayFilter~setOverlayedImageCallback
 * @param {Error} error
 */

/**
 * Clear the image to be shown over each detected face. Stops overlaying the faces.
 *
 * @param {module:kwsMediaApi/filters~FaceOverlayFilter.unsetOverlayedImageCallback} [callback]
 *
 * @return {module:kwsMediaApi/filters~FaceOverlayFilter}
 *  The own media object
 */
FaceOverlayFilter.prototype.unsetOverlayedImage = function(callback){
  this.invoke('unsetOverlayedImage', callback);

  return this;
};
/**
 * @callback FaceOverlayFilter~unsetOverlayedImageCallback
 * @param {Error} error
 */


/**
 * @type module:kwsMediaApi/filters~FaceOverlayFilter.constructorParams
 *
 * @property {MediaPipeline} mediaPipeline
 *  pipeline to which this :rom:cls:`Filter` belons
 */
FaceOverlayFilter.constructorParams = {
  mediaPipeline: {
    type: 'MediaPipeline',
    required: true
  },
};

/**
 * @type   module:kwsMediaApi/filters~FaceOverlayFilter.events
 * @extend module:kwsMediaApi~Filter.events
 */
FaceOverlayFilter.events = [];
FaceOverlayFilter.events.concat(Filter.events);


module.exports = FaceOverlayFilter;


FaceOverlayFilter.check = function(key, value)
{
  if(!(value instanceof FaceOverlayFilter))
    throw SyntaxError(key+' param should be a FaceOverlayFilter, not '+typeof value);
};
