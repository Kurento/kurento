/*
 * (C) Copyright 2013-2015 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

/**
 * Media API for the Kurento Web SDK
 *
 * @module kurentoClient
 *
 * @copyright 2013-2015 Kurento (http://kurento.org/)
 * @license ALv2
 */

require('error-tojson');

var checkType = require('./checkType');

var disguise = require('./disguise')
var MediaObjectCreator = require('./MediaObjectCreator');
var register = require('./register');
var TransactionsManager = require('./TransactionsManager');

exports.checkType = checkType;
exports.disguise = disguise;
exports.MediaObjectCreator = MediaObjectCreator;
exports.register = register;
exports.TransactionsManager = TransactionsManager;

// Export KurentoClient

var KurentoClient = require('./KurentoClient');

module.exports = KurentoClient;
KurentoClient.KurentoClient = KurentoClient;

// Ugly hack due to circular references

KurentoClient.checkType = checkType;
KurentoClient.disguise = disguise;
KurentoClient.MediaObjectCreator = MediaObjectCreator;
KurentoClient.register = register;
KurentoClient.TransactionsManager = TransactionsManager;

// Register Kurento basic elements

register('kurento-client-core')
register('kurento-client-elements')
register('kurento-client-filters')
