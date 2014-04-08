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

var extend   = require('extend');
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

var Filter = require('../Filter');


/**
 * ChromaFilter interface. This type of :rom:cls:`Filter` makes transparent a colour
range in the top layer, revealing another image behind
 *
 * @class   module:kwsMediaApi/filters~ChromaFilter
 * @extends module:kwsMediaApi~Filter
 */

/**
 * Create a :rom:cls:`ChromaFilter`
 *
 * @constructor
 *
 * @param {string} id
 * @param {module:kwsMediaApi~MediaContainer} parent
 * @param {module:kwsMediaApi~MediaPipeline} pipeline
 * @param {module:kwsMediaApi/filters~ChromaFilter.constructorParams} params
 */
function ChromaFilter(id, parent, pipeline, params)
{
  Filter.call(this, id, parent, pipeline, params);
};
inherits(ChromaFilter, Filter);


/**
 * Sets the image to show on the detected chroma surface.
 *
 * @param {String} uri
 *  URI where the image is located
 *
 * @param {module:kwsMediaApi/filters~ChromaFilter.setBackgroundCallback} [callback]
 *
 * @return {module:kwsMediaApi/filters~ChromaFilter}
 *  The own media object
 */
ChromaFilter.prototype.setBackground = function(uri, callback){
  checkType('String', 'uri', uri, {required: true});

  var params = {
    uri: uri,
  };

  this.invoke('setBackground', params, callback);

  return this;
};
/**
 * @callback ChromaFilter~setBackgroundCallback
 * @param {Error} error
 */

/**
 * Clears the image used to be shown behind the chroma surface.
 *
 * @param {module:kwsMediaApi/filters~ChromaFilter.unsetBackgroundCallback} [callback]
 *
 * @return {module:kwsMediaApi/filters~ChromaFilter}
 *  The own media object
 */
ChromaFilter.prototype.unsetBackground = function(callback){
  this.invoke('unsetBackground', callback);

  return this;
};
/**
 * @callback ChromaFilter~unsetBackgroundCallback
 * @param {Error} error
 */

/**
 * @type   module:kwsMediaApi/filters~ChromaFilter.constructorParams
 * @extend module:kwsMediaApi~Filter.constructorParams
 *
 * @property {String} [backgroundImage]
 *  url of image to be used to replace the detected background
 *
 * @property {MediaPipeline} mediaPipeline
 *  the :rom:cls:`MediaPipeline` to which the filter belongs
 *
 * @property {WindowParam} window
 *  Window of replacement for the :rom:cls:`ChromaFilter`
 */
ChromaFilter.constructorParams = {
  backgroundImage: {
    type: 'String',
  },

  mediaPipeline: {
    type: 'MediaPipeline',
    required: true
  },

  window: {
    type: 'WindowParam',
    required: true
  },
};
extend(ChromaFilter.constructorParams, Filter.constructorParams);

/**
 * @type   module:kwsMediaApi/filters~ChromaFilter.events
 * @extend module:kwsMediaApi~Filter.events
 */
ChromaFilter.events = [];
ChromaFilter.events.concat(Filter.events);


/**
 *
 *
 * @param {module:kwsMediaApi/filters~ChromaFilter.constructorParams} params
 */
ChromaFilter.create = function(pipeline, params, callback)
{
  pipeline.createMediaElement('ChromaFilter', params, callback);
};


module.exports = ChromaFilter;
