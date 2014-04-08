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
 * @module kwsMediaApi/filters
 *
 * @copyright 2014 Kurento (http://kurento.org/)
 * @license LGPL
 */

var ChromaFilter = require('./ChromaFilter');
var CrowdDetectorFilter = require('./CrowdDetectorFilter');
var FaceOverlayFilter = require('./FaceOverlayFilter');
var GStreamerFilter = require('./GStreamerFilter');
var JackVaderFilter = require('./JackVaderFilter');
var PlateDetectorFilter = require('./PlateDetectorFilter');
var PointerDetectorAdvFilter = require('./PointerDetectorAdvFilter');
var PointerDetectorFilter = require('./PointerDetectorFilter');
var ZBarFilter = require('./ZBarFilter');


exports.ChromaFilter = ChromaFilter;
exports.CrowdDetectorFilter = CrowdDetectorFilter;
exports.FaceOverlayFilter = FaceOverlayFilter;
exports.GStreamerFilter = GStreamerFilter;
exports.JackVaderFilter = JackVaderFilter;
exports.PlateDetectorFilter = PlateDetectorFilter;
exports.PointerDetectorAdvFilter = PointerDetectorAdvFilter;
exports.PointerDetectorFilter = PointerDetectorFilter;
exports.ZBarFilter = ZBarFilter;
