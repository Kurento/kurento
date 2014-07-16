index.js
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

<#list model.remoteClasses?sort_by("name") as remoteClass>
  <#if !remoteClass.abstract>
var ${remoteClass.name} = require('./${remoteClass.name}');
  </#if>
</#list>


<#list model.remoteClasses?sort_by("name") as remoteClass>
  <#if !remoteClass.abstract>
exports.${remoteClass.name} = ${remoteClass.name};
  </#if>
</#list>

exports.abstracts    = require('./abstracts');
exports.complexTypes = require('./complexTypes');
