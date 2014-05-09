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
 * This type of :rom:cls:`Filter` detects UI pointers in a video feed.
 *
 * @class   module:kwsMediaApi/filters~PointerDetectorAdvFilter
 * @extends module:kwsMediaApi~Filter
 */

/**
 * Builder for the :rom:cls:`PointerDetectorAdvFilter`.
 *
 * @constructor
 *
 * @param {string} id
 */
function PointerDetectorAdvFilter(id)
{
  Filter.call(this, id);
};
inherits(PointerDetectorAdvFilter, Filter);


/**
 *  Adds a new detection window for the filter to detect pointers entering or exiting the window
 *
 * @param {PointerDetectorWindowMediaParam} window
 *  The window to be added
 *
 * @param {module:kwsMediaApi/filters~PointerDetectorAdvFilter.addWindowCallback} [callback]
 *
 * @return {module:kwsMediaApi/filters~PointerDetectorAdvFilter}
 *  The own media object
 */
PointerDetectorAdvFilter.prototype.addWindow = function(window, callback){
  checkType('PointerDetectorWindowMediaParam', 'window', window, {required: true});

  var params = {
    window: window,
  };

  this.invoke('addWindow', params, callback);

  return this;
};
/**
 * @callback PointerDetectorAdvFilter~addWindowCallback
 * @param {Error} error
 */

/**
 * Removes all pointer detector windows
 *
 * @param {module:kwsMediaApi/filters~PointerDetectorAdvFilter.clearWindowsCallback} [callback]
 *
 * @return {module:kwsMediaApi/filters~PointerDetectorAdvFilter}
 *  The own media object
 */
PointerDetectorAdvFilter.prototype.clearWindows = function(callback){
  this.invoke('clearWindows', callback);

  return this;
};
/**
 * @callback PointerDetectorAdvFilter~clearWindowsCallback
 * @param {Error} error
 */

/**
 * Removes a window from the list to be monitored
 *
 * @param {String} windowId
 *  the id of the window to be removed
 *
 * @param {module:kwsMediaApi/filters~PointerDetectorAdvFilter.removeWindowCallback} [callback]
 *
 * @return {module:kwsMediaApi/filters~PointerDetectorAdvFilter}
 *  The own media object
 */
PointerDetectorAdvFilter.prototype.removeWindow = function(windowId, callback){
  checkType('String', 'windowId', windowId, {required: true});

  var params = {
    windowId: windowId,
  };

  this.invoke('removeWindow', params, callback);

  return this;
};
/**
 * @callback PointerDetectorAdvFilter~removeWindowCallback
 * @param {Error} error
 */

/**
 * This method allows to calibrate the tracking color.

The new tracking color will be the color of the object in the colorCalibrationRegion.
 *
 * @param {module:kwsMediaApi/filters~PointerDetectorAdvFilter.trackColorFromCalibrationRegionCallback} [callback]
 *
 * @return {module:kwsMediaApi/filters~PointerDetectorAdvFilter}
 *  The own media object
 */
PointerDetectorAdvFilter.prototype.trackColorFromCalibrationRegion = function(callback){
  this.invoke('trackColorFromCalibrationRegion', callback);

  return this;
};
/**
 * @callback PointerDetectorAdvFilter~trackColorFromCalibrationRegionCallback
 * @param {Error} error
 */


/**
 * @type module:kwsMediaApi/filters~PointerDetectorAdvFilter.constructorParams
 *
 * @property {WindowParam} calibrationRegion
 *  region to calibrate the filter
 *
 * @property {MediaPipeline} mediaPipeline
 *  the :rom:cls:`MediaPipeline` to which the filter belongs
 *
 * @property {PointerDetectorWindowMediaParam} [windows]
 *  list of detection windows for the filter.
 */
PointerDetectorAdvFilter.constructorParams = {
  calibrationRegion: {
    type: 'WindowParam',
    required: true
  },

  mediaPipeline: {
    type: 'MediaPipeline',
    required: true
  },

  windows: {
    type: 'PointerDetectorWindowMediaParam',
    isList: true,
  },
};

/**
 * @type   module:kwsMediaApi/filters~PointerDetectorAdvFilter.events
 * @extend module:kwsMediaApi~Filter.events
 */
PointerDetectorAdvFilter.events = ['WindowIn', 'WindowOut'];
PointerDetectorAdvFilter.events.concat(Filter.events);


module.exports = PointerDetectorAdvFilter;


PointerDetectorAdvFilter.check = function(key, value)
{
  if(!(value instanceof PointerDetectorAdvFilter))
    throw SyntaxError(key+' param should be a PointerDetectorAdvFilter, not '+typeof value);
};
