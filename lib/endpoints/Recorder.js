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
 * @module KwsMedia/elements
 *
 * @copyright 2014 Kurento (http://kurento.org/)
 * @license LGPL
 */

var Uri = require('./Uri');


var checkType = require('../checkType');


/**
 * @class
 * @extends module:KwsMedia/elements~Uri
 *
 * @param objectRef
 * @param {module:KwsMedia~MediaContainer} parent
 * @param {module:KwsMedia~MediaPipeline} pipeline
 * @param {module:KwsMedia/elements~Recorder.ConstructorParams} params
 */
function Recorder(objectRef, parent, pipeline, params)
{
  Uri.call(this, objectRef, parent, pipeline, params);
};
Recorder.prototype.__proto__   = Uri.prototype;
Recorder.prototype.constructor = Recorder;


/**
 * 
 * @param {module:KwsMedia/elements~Recorder.ConstructorParams} params
 *
 * @throws {SyntaxError}
 *
 * @returns {module:KwsMedia/elements~Recorder.ConstructorParams}
 */
Recorder.checkparams = function(params)
{
  /**
   * @type {module:KwsMedia/elements~Recorder.ConstructorParams}
   */
  var result = Uri.checkparams(params);

  // check MediaObject params
  for(var key in params)
  {
    var value = params[key];

    switch(key)
    {
      case 'profileType':
        checkType('MediaProfile', key, value);
      break;

      case 'stopOnEOS':
        checkType('boolean', key, value);
      break;

      default:
        continue;
    };

    result[key] = value;
  };

  return result;
};


/**
 * @typedef module:KwsMedia/elements~Recorder.ConstructorParams
 *
 * @property {Boolean} excludeFromGC
 * @property {external:Number} garbageCollectorPeriod
 *
 * @property {external:String} uri
 *
 * @property {external:String} profileType
 * @property {Boolean} stopOnEOS
 */


module.exports = Recorder;