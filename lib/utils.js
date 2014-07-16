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


function noop(error)
{
  if(error) console.error(error);
};


/**
 * Define a callback as the continuation of a promise
 */
function promiseCallback(promise, callback)
{
  if(callback)
  {
    function callback2(error, result)
    {
      try
      {
        callback(error, result);
      }
      catch(exception)
      {
        // Show the exception in the console with its full stack trace
        console.error(exception);
        throw exception;
      }
    };

    promise.then(function(result)
    {
      callback2(null, result);
    },
    callback2);
  };
};


exports.noop            = noop;
exports.promiseCallback = promiseCallback;
