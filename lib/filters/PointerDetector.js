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
 * @class   module:KwsMedia/filters~PointerDetector
 * @extends module:KwsMedia~MediaFilter
 *
 * @param id
 * @param parent
 * @param pipeline
 * @param {module:KwsMedia/filters~PointerDetector.ConstructorParams} params
 */
function PointerDetector(id, parent, pipeline, params)
{
  MediaFilter.call(this, id, parent, pipeline, params);
};
PointerDetector.prototype.__proto__   = MediaFilter.prototype;
PointerDetector.prototype.constructor = PointerDetector;


PointerDetector.prototype.addWindow = function(window, callback)
{
  checkType('MediaPointerDetectorWindow', 'window', window);

  this.invoke('addWindow', {window: window}, callback);
};

PointerDetector.prototype.removeWindow = function(windowID, callback)
{
  if(windowID == undefined)
    throw SyntaxError('windowID param is required');

  this.invoke('removeWindow', {windowID: windowID}, callback);
};

PointerDetector.prototype.clearWindows = function(callback)
{
  this.invoke('clearWindows', callback);
};

PointerDetector.prototype.trackColorFromCalibrationRegion = function(callback)
{
  this.invoke('trackColorFromCalibrationRegion', callback);
};


/**
 * @type   module:KwsMedia/filters~PointerDetector.paramsScheme
 * @extend module:KwsMedia~MediaFilter.paramsScheme
 */
PointerDetector.paramsScheme =
{
  /**
   * @type Set_MediaPointerDetectorWindow
   */
  windows:
  {
    type: 'Set_MediaPointerDetectorWindow'
  }
};
extend(PointerDetector.paramsScheme, MediaFilter.paramsScheme);


PointerDetector.create = function(pipeline, params, callback)
{
  pipeline.createMediaElement('PointerDetectorFilter', params, callback);
};


module.exports = PointerDetector;