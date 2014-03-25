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
 * @module kwsMediaApi/endpoints
 *
 * @copyright 2014 Kurento (http://kurento.org/)
 * @license LGPL
 */

var HttpGet  = require('./HttpGet');
var HttpPost = require('./HttpPost');
var Player   = require('./Player');
var Recorder = require('./Recorder');
var Rtp      = require('./Rtp');
var WebRtc   = require('./WebRtc');


exports.HttpGetEndpoint  = HttpGet;
exports.HttpPostEndpoint = HttpPost;
exports.PlayerEndpoint   = Player;
exports.RecorderEndpoint = Recorder;
exports.RtpEndpoint      = Rtp;
exports.WebRtcEndpoint   = WebRtc;
