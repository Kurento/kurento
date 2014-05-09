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

var extend = require('extend');

/**
 * Media API for the Kurento Web SDK
 *
 * @module kwsMediaApi
 *
 * @copyright 2014 Kurento (http://kurento.org/)
 * @license LGPL
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
    throw SyntaxError(key+' param should be an Array of '+type+', not '+typeof value);

  for(var i=0, item; item=value[i]; i++)
    checkType(type, key+'['+i+']', item);
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


// Checker functions

function checkType(type, key, value, options)
{
  options = options || {};

  if(value != undefined)
  {
    if(options.isArray)
      return checkArray(type, key, value);

    var checker = checkType[type];
    if(checker) return checker(key, value);

    console.warn("Could not check "+key+", unknown type "+type);
//    throw TypeError("Could not check "+key+", unknown type "+type);
  }

  else if(options.required)
    throw SyntaxError(key+" param is required");

};

function checkParams(params, scheme, class_name)
{
  var result = {};

  // check MediaObject params
  for(var key in scheme)
  {
    var value = params[key];

    var s = scheme[key];

    var options = {required: s.required, isArray: s.isList};

    checkType(s.type, key, value, options);

    if(value == undefined) continue;

    result[key] = value;
    delete params[key];
  };

  if(Object.keys(params).length)
    console.warn('Unused params for '+class_name+':', params);

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

    var options = {required: param.required, isArray: param.isList};

    checkType(param.type, key, value, options);

    result[key] = value;
  }

  var params = callparams.slice(index);
  if(params.length)
    console.warning('Unused params:', params);

  return result;
};


module.exports = checkType;

checkType.checkParams = checkParams;


// Basic types

checkType.boolean = checkBoolean;
checkType.double  = checkDouble;
checkType.int     = checkInteger;
checkType.Object  = checkObject;
checkType.String  = checkString;


// Complex types

var complexTypes = require('./complexTypes');

extend(checkType, complexTypes);


// Elements

function addCheckers(elements)
{
  for(var key in elements)
  {
    var check = elements[key].check;
    if(check) checkType[key] = check;
  };
};


var core      = require('./core');
var endpoints = require('./endpoints');
var filters   = require('./filters');
var hubs      = require('./hubs');

addCheckers(core);
addCheckers(endpoints);
addCheckers(filters);
addCheckers(hubs);


var MediaElement = require('./core/MediaElement');

checkType.MediaElement = MediaElement.check;
