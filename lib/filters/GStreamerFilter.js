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
 * New node file
 */

var MediaFilter = require('../MediaFilter');


var checkType = require('../checkType');


function GStreamerFilter(objectRef, parent, pipeline, params)
{
  MediaFilter.call(this, objectRef, parent, pipeline, params);
};
GStreamerFilter.prototype.__proto__   = MediaFilter.prototype;
GStreamerFilter.prototype.constructor = GStreamerFilter;


/**
 * 
 * @param {MediaUriEndPointConstructorParams} params
 *
 * @throws {SyntaxError}
 *
 * @returns {MediaUriEndPointConstructorParams}
 */
GStreamerFilter.checkparams = function(params)
{
  var result = MediaFilter.checkparams(params);

  // check MediaObject params
  for(var key in params)
  {
    var value = params[key];

    switch(key)
    {
      case 'command':
        checkType['String'](key, value, true);
      break;

      default:
        continue;
    };

    result[key] = value;
  };

  return result;
};


module.exports = GStreamerFilter;