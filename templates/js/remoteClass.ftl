${remoteClass.name}.js
<#if remoteClass.extends??>
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

var extend   = require('extend');
var inherits = require('inherits');

var ${remoteClass.extends.name} = require('../${remoteClass.extends.name}');

/**
 * @class   module:kwsMediaApi/filters~${remoteClass.name}
 * @extends module:kwsMediaApi~${remoteClass.extends.name}
 *
 * @param id
 * @param parent
 * @param pipeline
 * @param {module:kwsMediaApi/filters~${remoteClass.name}.ConstructorParams} params
 */
function ${remoteClass.name}(id, parent, pipeline, params)
{
  ${remoteClass.extends.name}.call(this, id, parent, pipeline, params);
};
inherits(${remoteClass.name}, ${remoteClass.extends.name});


/**
 * @type   module:kwsMediaApi/filters~${remoteClass.name}.paramsScheme
 * @extend module:kwsMediaApi~${remoteClass.extends.name}.paramsScheme
 */
${remoteClass.name}.paramsScheme =
{
  <#if remoteClass.constructors[0]??>
  <#list remoteClass.constructors[0].params as param>
  ${param.name}:
  {
    type: ${param.type.name}
    required: <#if param.optional>false<#else>true</#if>
  }
  </#list>
  </#if>
};
extend(${remoteClass.name}.paramsScheme, ${remoteClass.extends.name}.paramsScheme);


GStreamer.create = function(pipeline, params, callback)
{
  pipeline.createMediaElement('${remoteClass.name}Filter', params, callback);
};

module.exports = ${remoteClass.name};
</#if>