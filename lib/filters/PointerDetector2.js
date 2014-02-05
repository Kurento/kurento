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
 * @class   module:KwsMedia/filters~PointerDetector2
 * @extends module:KwsMedia~MediaFilter
 *
 * @param objectRef
 * @param parent
 * @param pipeline
 * @param {module:KwsMedia/filters~PointerDetector2.ConstructorParams} params
 */
function PointerDetector2(objectRef, parent, pipeline, params)
{
  MediaFilter.call(this, objectRef, parent, pipeline, params);
};
PointerDetector2.prototype.__proto__   = MediaFilter.prototype;
PointerDetector2.prototype.constructor = PointerDetector2;


/**
 * 
 * @param {MediaPointerDetectorWindow} window
 * @param callback
 */
PointerDetector2.prototype.addWindow = function(window, callback)
{
  checkType('MediaPointerDetectorWindow', 'window', window);

  this.invoke('addWindow', {window: window}, callback);
};

/**
 * 
 * @param windowID
 * @param callback
 */
PointerDetector2.prototype.removeWindow = function(windowID, callback)
{
  if(windowID == undefined)
    throw SyntaxError('windowID param is required');

  this.invoke('removeWindow', {windowID: windowID}, callback);
};

/**
 * 
 * @param callback
 */
PointerDetector2.prototype.clearWindows = function(callback)
{
  this.invoke('clearWindows', callback);
};

/**
 * 
 * @param callback
 */
PointerDetector2.prototype.trackColorFromCalibrationRegion = function(callback)
{
  this.invoke('trackColorFromCalibrationRegion', callback);
};


/**
 * @type   module:KwsMedia/filters~PointerDetector2.paramsScheme
 * @extend module:KwsMedia~MediaFilter.paramsScheme
 */
PointerDetector2.paramsScheme =
{
  /**
   * @type MediaImageRegion
   */
  colorCalibrationRegion:
  {
    type: 'MediaImageRegion',
    required: true
  },

  /**
   * @type MediaPointerDetectorWindowSet
   */
  windowSet:
  {
    type: 'MediaPointerDetectorWindowSet'
  }
};
extend(PointerDetector2.paramsScheme, MediaFilter.paramsScheme);


PointerDetector2.create = function(pipeline, params, callback)
{
  pipeline.createMediaElement('PointerDetector2Filter', params, callback);
};


module.exports = PointerDetector2;