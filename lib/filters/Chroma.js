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

var extend = require('extend');

var checkType = require('../checkType');

var MediaFilter = require('../MediaFilter');


/**
 * @class   module:KwsMedia/filters~Chroma
 * @extends module:KwsMedia~MediaFilter
 *
 * @param id
 * @param {module:KwsMedia~MediaContainer} parent
 * @param {module:KwsMedia~MediaPipeline} pipeline
 * @param {module:KwsMedia/filters~Chroma.ConstructorParams} params
 */
function Chroma(id, parent, pipeline, params)
{
  MediaFilter.call(this, id, parent, pipeline, params);
};
Chroma.prototype.__proto__   = MediaFilter.prototype;
Chroma.prototype.constructor = Chroma;


Chroma.prototype.setBackground = function(backgroundImage, callback)
{
  checkType('KmsMediaChromaBackgroundImage', 'backgroundImage', backgroundImage);

  this.invoke('setBackground', {backgroundImage: backgroundImage}, callback);
};

Chroma.prototype.unsetBackground = function(callback)
{
  this.invoke('unsetBackground', callback);
};


/**
 * @type   module:KwsMedia/filters~Chroma.paramsScheme
 * @extend module:KwsMedia~MediaFilter.paramsScheme
 */
Chroma.paramsScheme =
{
  /**
   * @type KmsMediaChromaColorCalibrationArea
   */
  window:
  {
    type: 'WindowParam',
    required: true
  },

  /**
   * @type KmsMediaChromaBackgroundImage
   */
  backgroundImage:
  {
    type: 'String'
  }
};
extend(Chroma.paramsScheme, MediaFilter.paramsScheme);


Chroma.create = function(pipeline, params, callback)
{
  pipeline.createMediaElement('ChromaFilter', params, callback);
};


module.exports = Chroma;