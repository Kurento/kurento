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

var GStreamerFilter = KwsMedia.filters.GStreamerFilter;


function MirrorFilter(id, parent, pipeline, params)
{
  GStreamerFilter.call(this, id, parent, pipeline, params);
};
MirrorFilter.prototype.__proto__   = GStreamerFilter.prototype;
MirrorFilter.prototype.constructor = MirrorFilter;


MirrorFilter.paramsScheme = GStreamerFilter.paramsScheme;


MirrorFilter.create = function(pipeline, callback)
{
  var params =
  {
    command: 'videoflip method=4'
  };

  GStreamerFilter.create.call(this, pipeline, params, callback);
};