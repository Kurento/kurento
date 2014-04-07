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

var extend   = require('extend');
var inherits = require('inherits');

var checkType = require('../checkType');

var MediaFilter = require('../MediaFilter');


/**
 * @class   module:kwsMediaApi/filters~PointerDetectorAdv
 * @extends module:kwsMediaApi~MediaFilter
 *
 * @param id
 * @param parent
 * @param pipeline
 * @param {module:kwsMediaApi/filters~PointerDetector2.ConstructorParams} params
 */
function PointerDetectorAdv(id, parent, pipeline, params)
{
  MediaFilter.call(this, id, parent, pipeline, params);
};
inherits(PointerDetectorAdv, MediaFilter);


/**
 * 
 * @param {MediaPointerDetectorWindow} window
 * @param callback
 */
PointerDetectorAdv.prototype.addWindow = function(window, callback)
{
  checkType('PointerDetectorWindowMediaParam', 'window', window);

  this.invoke('addWindow', {window: window}, callback);
};

/**
 * 
 * @param callback
 */
PointerDetectorAdv.prototype.clearWindows = function(callback)
{
  this.invoke('clearWindows', callback);
};

/**
 * 
 * @param windowID
 * @param callback
 */
PointerDetectorAdv.prototype.removeWindow = function(windowID, callback)
{
  checkType('String', 'windowID', windowID, true);

  this.invoke('removeWindow', {windowID: windowID}, callback);
};

/**
 * 
 * @param callback
 */
PointerDetectorAdv.prototype.trackColorFromCalibrationRegion = function(callback)
{
  this.invoke('trackColorFromCalibrationRegion', callback);
};


/**
 * @type   module:kwsMediaApi/filters~PointerDetectorAdv.paramsScheme
 * @extend module:kwsMediaApi~MediaFilter.paramsScheme
 */
PointerDetectorAdv.paramsScheme =
{
  /**
   * @type WindowParam
   */
  calibrationRegion:
  {
    type: 'WindowParam',
    required: true
  },

  /**
   * @type PointerDetectorWindowMediaParam[]
   */
  windows:
  {
    type: 'PointerDetectorWindowMediaParam[]'
  }
};
extend(PointerDetectorAdv.paramsScheme, MediaFilter.paramsScheme);


PointerDetectorAdv.eventsScheme =
{
  'WindowIn':
  {
    windowID:
    {
      type: 'string'
    }
  },

  'WindowOut':
  {
    windowID:
    {
      type: 'string'
    }
  }
};


PointerDetectorAdv.create = function(pipeline, params, callback)
{
  pipeline.createMediaElement('PointerDetectorAdvFilter', params, callback);
};


module.exports = PointerDetectorAdv;
