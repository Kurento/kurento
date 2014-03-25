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

/**
 * Media API for the Kurento Web SDK
 *
 * @module kwsMediaApi/elements
 *
 * @copyright 2014 Kurento (http://kurento.org/)
 * @license LGPL
 */

var Session = require('./Session');

var extend   = require('extend');
var inherits = require('inherits');


var checkType = require('../checkType');


/**
 * @class
 * @extends module:kwsMediaApi/elements~Session
 *
 * @param id
 * @param {module:kwsMediaApi~MediaContainer} parent
 * @param {module:kwsMediaApi~MediaPipeline} pipeline
 * @param {module:kwsMediaApi/elements~Http.paramsScheme} params
 */
function Http(id, parent, pipeline, params)
{
  Session.call(this, id, parent, pipeline);
};
inherits(Http, Session);

/**
 * Get the {URL} where to download or upload the data
 *
 * @param {} callback
 *
 * @returns {external:String}
 */
Http.prototype.getUrl = function(callback)
{
  return this.invoke('getUrl', callback);
};


Http.paramsScheme =
{
  disconnectionTimeout:
  {
    type: 'Integer'
  }
};
extend(Http.paramsScheme, Session.paramsScheme);


module.exports = Http;
