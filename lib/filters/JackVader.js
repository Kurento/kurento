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

var MediaFilter = require('../MediaFilter');


/**
 * @class   module:KwsMedia/filters~JackVader
 * @extends module:KwsMedia~MediaFilter
 *
 * @param id
 * @param parent
 * @param pipeline
 * @param {module:KwsMedia/filters~JackVader.ConstructorParams} params
 */
function JackVader(id, parent, pipeline, params)
{
  MediaFilter.call(this, id, parent, pipeline, params);
};
JackVader.prototype.__proto__   = MediaFilter.prototype;
JackVader.prototype.constructor = JackVader;


/**
 * @see {@link module:KwsMedia~MediaFilter.paramsScheme}
 */
JackVader.paramsScheme = MediaFilter.paramsScheme;


JackVader.create = function(pipeline, params, callback)
{
  pipeline.createMediaElement('JackVaderFilter', params, callback);
};


module.exports = JackVader;