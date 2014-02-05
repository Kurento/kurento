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
 * @module KwsMedia/filters *
 * @copyright 2014 Kurento (http://kurento.org/)
 * @license LGPL
 */

var MediaFilter = require('../MediaFilter');


/**
 * @class
 * @extends module:KwsMedia/filters~ZBarFilter
 *
 * @param objectRef
 * @param parent
 * @param pipeline
 * @param {module:KwsMedia~MediaFilter.ConstructorParams} params
 */
function ZBarFilter(objectRef, parent, pipeline, params)
{
  MediaFilter.call(this, objectRef, parent, pipeline, params);
};
ZBarFilter.prototype.__proto__   = MediaFilter.prototype;
ZBarFilter.prototype.constructor = ZBarFilter;


/**
 * @see {@link module:KwsMedia~MediaFilter.checkparams}
 */
ZBarFilter.checkparams = MediaFilter.checkparams;


module.exports = ZBarFilter;