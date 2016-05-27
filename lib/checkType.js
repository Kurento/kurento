/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

/**
 * Number.isInteger() polyfill
 * @function external:Number#isInteger
 * @see {@link https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Number/isInteger Number.isInteger}
 */
if (!Number.isInteger) {
  Number.isInteger = function isInteger(nVal) {
    return typeof nVal === "number" && isFinite(nVal) && nVal > -
      9007199254740992 && nVal < 9007199254740992 && Math.floor(nVal) ===
      nVal;
  };
}

function ChecktypeError(key, type, value) {
  return SyntaxError(key + ' param should be a ' + (type.name || type) +
    ', not ' + value.constructor.name);
}

//
// Basic types
//

function checkArray(type, key, value) {
  if (!(value instanceof Array))
    throw ChecktypeError(key, 'Array of ' + type, value);

  value.forEach(function (item, i) {
    checkType(type, key + '[' + i + ']', item);
  })
};

function checkBoolean(key, value) {
  if (typeof value != 'boolean')
    throw ChecktypeError(key, Boolean, value);
};

function checkNumber(key, value) {
  if (typeof value != 'number')
    throw ChecktypeError(key, Number, value);
};

function checkInteger(key, value) {
  if (!Number.isInteger(value))
    throw ChecktypeError(key, 'Integer', value);
};

function checkObject(key, value) {
  if (typeof value != 'object')
    throw ChecktypeError(key, Object, value);
};

function checkString(key, value) {
  if (typeof value != 'string')
    throw ChecktypeError(key, String, value);
};

// Checker functions

function checkType(type, key, value, options) {
  options = options || {};

  if (value != undefined) {
    if (options.isArray)
      return checkArray(type, key, value);

    var checker = checkType[type];
    if (checker) return checker(key, value);

    console.warn("Could not check " + key + ", unknown type " + type);
    //    throw TypeError("Could not check "+key+", unknown type "+type);
  } else if (options.required)
    throw SyntaxError(key + " param is required");

};

function checkParams(params, scheme, class_name) {
  var result = {};

  // check MediaObject params
  for (var key in scheme) {
    var value = params[key];

    var s = scheme[key];

    checkType(s.type, key, value, s);

    if (value == undefined) continue;

    result[key] = value;
    delete params[key];
  };

  return result;
};

function checkMethodParams(callparams, method_params) {
  var result = {};

  var index = 0,
    param;
  for (; param = method_params[index]; index++) {
    var key = param.name;
    var value = callparams[index];

    checkType(param.type, key, value, param);

    result[key] = value;
  }

  var params = callparams.slice(index);
  if (params.length)
    console.warning('Unused params:', params);

  return result;
};

module.exports = checkType;

checkType.checkArray = checkArray;
checkType.checkParams = checkParams;
checkType.ChecktypeError = ChecktypeError;

// Basic types

checkType.boolean = checkBoolean;
checkType.double = checkNumber;
checkType.float = checkNumber;
checkType.int = checkInteger;
checkType.Object = checkObject;
checkType.String = checkString;
