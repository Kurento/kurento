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

var Uri = require('./Uri');

var extend   = require('extend');
var inherits = require('inherits');


var checkType = require('../checkType');


/**
 * @class
 * @extends module:kwsMediaApi/endpoints~Uri
 *
 * @param id
 * @param {module:kwsMediaApi~MediaContainer} parent
 * @param {module:kwsMediaApi~MediaPipeline} pipeline
 * @param {module:kwsMediaApi/elements~Recorder.paramsScheme} params
 */
function Recorder(id, parent, pipeline, params)
{
  Uri.call(this, id, parent, pipeline, params);
};
inherits(Recorder, Uri);


Recorder.prototype.record = function(callback)
{
  this.invoke('record', callback);
};


Recorder.paramsScheme =
{
  profileType:
  {
    type: 'MediaProfile'
  },
  stopOnEOS:
  {
    type: 'boolean'
  }
};
extend(Recorder.paramsScheme, Uri.paramsScheme);


Recorder.create = function(pipeline, params, callback)
{
  pipeline.createMediaElement('RecorderEndpoint', params, callback);
};


module.exports = Recorder;
