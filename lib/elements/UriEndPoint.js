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


var MediaElement = require('../MediaElement');


function UriEndPoint(objectRef, parent, pipeline, params)
{
  MediaElement.call(this, objectRef, parent, pipeline, params);
};
UriEndPoint.prototype.__proto__   = MediaElement.prototype;
UriEndPoint.prototype.constructor = UriEndPoint;


UriEndPoint.prototype.getUri = function(callback)
{
  return this.invoke('getUri', callback);
};

UriEndPoint.prototype.start = function(callback)
{
  return this.invoke('start', callback);
};

UriEndPoint.prototype.pause = function(callback)
{
  return this.invoke('pause', callback);
};

UriEndPoint.prototype.stop = function(callback)
{
  return this.invoke('stop', callback);
};


/**
 * 
 * @param {MediaUriEndPointConstructorParams} params
 *
 * @throws {SyntaxError}
 *
 * @returns {MediaUriEndPointConstructorParams}
 */
UriEndPoint.checkparams = function(params)
{
  /**
   * @type {MediaUriEndPointConstructorParams}
   */
  var result = MediaElement.checkparams(params);

  // check MediaObject params
  for(var key in params)
  {
    var value = params[key];

    switch(key)
    {
      case 'uri':
        if(typeof value != 'string')
          throw SyntaxError(key+" param should be String");
      break;

      default:
        continue;
    };

    result[key] = value;
  };

  return result;
};


/**
 * @type MediaUriEndPointConstructorParams
 *
 * @member {String} uri
 */


module.exports = UriEndPoint;