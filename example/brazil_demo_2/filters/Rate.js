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


function RateFilter(id)
{
  GStreamerFilter.call(this, id);
};
RateFilter.prototype.__proto__   = GStreamerFilter.prototype;
RateFilter.prototype.constructor = RateFilter;


RateFilter.constructorParams = GStreamerFilter.constructorParams;


RateFilter.create = function(pipeline, callback)
{
  var params =
  {
    command: 'videorate max-rate=15 average-period=200000000'
  };

  GStreamerFilter.create.call(this, pipeline, params, callback);
};
