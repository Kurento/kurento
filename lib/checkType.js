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
 * Number.isInteger() polyfill
 * @function external:Number#isInteger
 * @see {@link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Number/isInteger Number.isInteger}
 */
if (!Number.isInteger) {
  Number.isInteger = function isInteger (nVal) {
    return typeof nVal === "number" && isFinite(nVal) && nVal > -9007199254740992 && nVal < 9007199254740992 && Math.floor(nVal) === nVal;
  };
}


//
// Basic types
//

function checkArray(type, key, value)
{
  if(!(value instanceof Array))
    throw SyntaxError(key+' param should be an Array, not '+typeof value);
  for(var i=0, item; item=value[i]; i++)
    this[type](key+'['+i+']', item);
};

function checkBoolean(key, value)
{
  if(typeof value != 'boolean')
    throw SyntaxError(key+' param should be a Boolean, not '+typeof value);
};

function checkDouble(key, value)
{
  if(typeof value != 'number')
    throw SyntaxError(key+' param should be a Float, not '+typeof value);
};

function checkInteger(key, value)
{
  if(!Number.isInteger(value))
    throw SyntaxError(key+' param should be an Integer, not '+typeof value);
};

function checkObject(key, value)
{
  if(typeof value != 'object')
    throw SyntaxError(key+' param should be an Object, not '+typeof value);
};

function checkString(key, value)
{
  if(typeof value != 'string')
    throw SyntaxError(key+' param should be a String, not '+typeof value);
};


//
// Kurento specific ones
//

function checkKmsMediaChromaBackgroundImage(key, value)
{
  checkString(key+'.uri', value.uri, true);
};

function checkKmsMediaChromaColorCalibrationArea(key, value)
{
  checkInteger(key+'.x',      value.x);
  checkInteger(key+'.y',      value.y);
  checkInteger(key+'.width',  value.width);
  checkInteger(key+'.height', value.height);
};


function checkMediaMuxer(key, value)
{
  if(typeof value != 'string')
    throw SyntaxError(key+' param should be a String, not '+typeof value);
  if(!value.match('WEBM|MP4'))
    throw SyntaxError(key+' param is not one of WEBM or MP4 ('+value+')');
};

function checkWindowParam(key, value)
{
  checkInteger(key+'.height',          value.height);
  checkInteger(key+'.width',           value.width);
  checkInteger(key+'.topRightCornerX', value.topRightCornerX);
  checkInteger(key+'.topRightCornerY', value.topRightCornerY);
};

function checkPointerDetectorWindowMediaParam(key, value)
{
  checkString (key+'.id',          value.id);
  checkInteger(key+'.height',      value.height);
  checkInteger(key+'.width',       value.width);
  checkInteger(key+'.upperRightX', value.upperRightX);
  checkInteger(key+'.upperRightY', value.upperRightY);

  checkType('String', key+'.image',             value.image);
  checkType('String', key+'.inactiveImage',     value.inactiveImage);
  checkType('double', key+'.imageTransparency', value.imageTransparency);
};

function checkMediaPointerDetectorWindowSet(key, value)
{
  checkSet_MediaPointerDetectorWindow(key+'.windows', value.windows);
};

function checkMediaProfile(key, value)
{
  checkObject(key, value);

  checkMediaMuxer(key+'.mediaMuxer', value.mediaMuxer);
};


function checkSet_MediaPointerDetectorWindow(key, value)
{
  checkArray('PointerDetectorWindowMediaParam', key, value);
};


// Checker functions

function checkType(type, key, value, required)
{
  if(value != undefined)
    checkType[type](key, value);

  else if(required)
    throw SyntaxError(key+" param is required");

};

function checkParams(params, klass)
{
  var scheme = klass.paramsScheme;

  var result = {};

  // check MediaObject params
  for(var key in scheme)
  {
    var value = params[key];

    checkType(scheme[key].type, key, value);

    if(value == undefined) continue;

    result[key] = value;
    delete params[key];
  };

  if(Object.keys(params).length)
    console.warn('Unused params for '+klass.name+':', params);

  return result;
};

function checkMethodParams(callparams, method_params)
{
  var result = {};

  var index=0, param;
  for(; param=method_params[index]; index++)
  {
    var key = param.name;
    var value = callparams[index];

    checkType(param.type, key, value, !param.optional);

    result[key] = value;
  }

  var params = callparams.slice(index);
  if(params.length)
    console.warning('Unused params:', params);

  return result;
};


module.exports = checkType;

checkType.checkParams = checkParams;

checkType.boolean = checkBoolean;
checkType.double  = checkDouble;
checkType.integer = checkInteger;
checkType.Object  = checkObject;
checkType.String  = checkString;

checkType.KmsMediaChromaBackgroundImage      = checkKmsMediaChromaBackgroundImage;
checkType.KmsMediaChromaColorCalibrationArea = checkKmsMediaChromaColorCalibrationArea;

checkType.MediaMuxer                      = checkMediaMuxer;
checkType.PointerDetectorWindowMediaParam = checkPointerDetectorWindowMediaParam;
checkType.MediaPointerDetectorWindowSet   = checkMediaPointerDetectorWindowSet;
checkType.MediaProfile                    = checkMediaProfile;

checkType.WindowParam = checkWindowParam;

checkType.Set_MediaPointerDetectorWindow = checkSet_MediaPointerDetectorWindow;
