<#assign remoteClasses_abstract=[]>
<#list model.remoteClasses?sort_by("name") as remoteClass>
  <#if remoteClass.abstract>
    <#assign remoteClasses_abstract=remoteClasses_abstract+[remoteClass.name]>
  </#if>
</#list>
<#if remoteClasses_abstract??>
abstracts/index.js
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

  <#list remoteClasses_abstract as remoteClass_name>
var ${remoteClass_name} = require('./${remoteClass_name}');
  </#list>


  <#list remoteClasses_abstract as remoteClass_name>
exports.${remoteClass_name} = ${remoteClass_name};
  </#list>
</#if>
