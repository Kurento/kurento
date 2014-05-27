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
 * This type of :rom:cls:`Filter` detects pointers in a video feed.
 *
 * @class   module:kwsMediaApi/filters~PointerDetectorFilter
 * @extends module:kwsMediaApi~Filter
 */

/**
 * Builder for the :rom:cls:`PointerDetectorFilter`.
 *
 * @constructor
 *
 * @param {string} id
 */
function PointerDetectorFilter(id)
{
  Filter.call(this, id);
};
inherits(PointerDetectorFilter, Filter);


/**
 * Adds a pointer detector window. When a pointer enters or exits this window, the filter will raise an event indicating so.
 *
 * @param {PointerDetectorWindowMediaParam} window
 *  the detection window
 *
 * @param {module:kwsMediaApi/filters~PointerDetectorFilter.addWindowCallback} [callback]
 *
 * @return {module:kwsMediaApi/filters~PointerDetectorFilter}
 *  The own media object
 */
PointerDetectorFilter.prototype.addWindow = function(window, callback){
  checkType('PointerDetectorWindowMediaParam', 'window', window, {required: true});

  var params = {
    window: window,
  };

  return this.invoke('addWindow', params, callback);
};
/**
 * @callback PointerDetectorFilter~addWindowCallback
 * @param {Error} error
 */

/**
 * Removes all pointer detector windows
 *
 * @param {module:kwsMediaApi/filters~PointerDetectorFilter.clearWindowsCallback} [callback]
 *
 * @return {module:kwsMediaApi/filters~PointerDetectorFilter}
 *  The own media object
 */
PointerDetectorFilter.prototype.clearWindows = function(callback){
  return this.invoke('clearWindows', callback);
};
/**
 * @callback PointerDetectorFilter~clearWindowsCallback
 * @param {Error} error
 */

/**
 * Removes a pointer detector window
 *
 * @param {String} windowId
 *  id of the window to be removed
 *
 * @param {module:kwsMediaApi/filters~PointerDetectorFilter.removeWindowCallback} [callback]
 *
 * @return {module:kwsMediaApi/filters~PointerDetectorFilter}
 *  The own media object
 */
PointerDetectorFilter.prototype.removeWindow = function(windowId, callback){
  checkType('String', 'windowId', windowId, {required: true});

  var params = {
    windowId: windowId,
  };

  return this.invoke('removeWindow', params, callback);
};
/**
 * @callback PointerDetectorFilter~removeWindowCallback
 * @param {Error} error
 */


/**
 * @type module:kwsMediaApi/filters~PointerDetectorFilter.constructorParams
 *
 * @property {MediaPipeline} mediaPipeline
 *  the :rom:cls:`MediaPipeline` to which the filter belongs
 *
 * @property {PointerDetectorWindowMediaParam} [windows]
 *  list of detection windows for the filter to detect pointers entering or exiting the window
 */
PointerDetectorFilter.constructorParams = {
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
 * @type   module:kwsMediaApi/filters~PointerDetectorFilter.events
 * @extend module:kwsMediaApi~Filter.events
 */
PointerDetectorFilter.events = ['WindowIn', 'WindowOut'];
PointerDetectorFilter.events.concat(Filter.events);


module.exports = PointerDetectorFilter;


PointerDetectorFilter.check = function(key, value)
{
  if(!(value instanceof PointerDetectorFilter))
    throw SyntaxError(key+' param should be a PointerDetectorFilter, not '+typeof value);
};
