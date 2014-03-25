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

var MediaFilter = require('../MediaFilter');


/**
 * @class   module:kwsMediaApi/filters~FaceOverlay
 * @extends module:kwsMediaApi~MediaFilter
 *
 * @param id
 * @param parent
 * @param pipeline
 * @param {module:kwsMediaApi/filters~FaceOverlay.ConstructorParams} params
 */
function FaceOverlay(id, parent, pipeline, params)
{
  MediaFilter.call(this, id, parent, pipeline, params);
};
FaceOverlay.prototype.__proto__   = MediaFilter.prototype;
FaceOverlay.prototype.constructor = FaceOverlay;


FaceOverlay.prototype.setOverlayedImage = function(uri,
    offsetXPercent, offsetYPercent, widthPercent, heightPercent, callback)
{
  var params =
  {
    uri:            uri,
    offsetXPercent: offsetXPercent,
    offsetYPercent: offsetYPercent,
    widthPercent:   widthPercent,
    heightPercent:  widthPercent
  };

  this.invoke('setOverlayedImage', params, callback);
};

FaceOverlay.prototype.unsetOverlayedImage = function(callback)
{
  this.invoke('unsetOverlayedImage', callback);
};


/**
 * @see {@link module:kwsMediaApi~MediaFilter.paramsScheme}
 */
FaceOverlay.paramsScheme = MediaFilter.paramsScheme;


FaceOverlay.create = function(pipeline, params, callback)
{
  pipeline.createMediaElement('FaceOverlayFilter', params, callback);
};


module.exports = FaceOverlay;
