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


<#assign remoteClasses_abstract=false>
<#list model.remoteClasses?sort_by("name") as remoteClass>
  <#if remoteClass.abstract>
    <#assign remoteClasses_abstract=true>
  <#else>
exports.${remoteClass.name} = ${remoteClass.name};
  </#if>
</#list>
<#assign complexTypes=model.complexTypes?? && model.complexTypes?has_content>
<#if remoteClasses_abstract || complexTypes>

  <#if remoteClasses_abstract>
exports.abstracts <#if complexTypes>   </#if>= require('./abstracts');
  </#if>
  <#if complexTypes>
exports.complexTypes = require('./complexTypes');
  </#if>
</#if>
