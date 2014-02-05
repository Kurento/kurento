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
 * @module KwsMedia/filters
 *
 * @copyright 2014 Kurento (http://kurento.org/)
 * @license LGPL
 */

var Chroma             = require('./Chroma');
var FaceOverlay        = require('./FaceOverlay');
var GStreamer          = require('./GStreamer');
var JackVader          = require('./JackVader');
var PointerDetector    = require('./PointerDetector');
var PointerDetectorAdv = require('./PointerDetectorAdv');
var ZBar               = require('./ZBar');


exports.ChromaFilter             = Chroma;
exports.FaceOverlayFilter        = FaceOverlay;
exports.GStreamerFilter          = GStreamer;
exports.JackVaderFilter          = JackVader;
exports.PointerDetectorFilter    = PointerDetector;
exports.PointerDetectorAdvFilter = PointerDetectorAdv;
exports.ZBarFilter               = ZBar;