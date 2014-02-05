/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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
 * Reserved for internal errors caused by miss-behavior of the media server
 *
 * @class   module:KwsMedia~MediaServerError
 * @extends Error
 *
 * @param {external:String} message - Description of the exception
 */
function MediaServerError(message)
{
  Error.call(this, message);

  /**
   * Error code
   *
   * @public
   * @readonly
   * @member {external:Number}
   */
  Object.defineProperty(this, "code", {value : 1});
};
MediaServerError.prototype.__proto__   = Error.prototype;
MediaServerError.prototype.constructor = MediaServerError;


module.exports = MediaServerError;