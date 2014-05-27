/*
 * (C) Copyright 2013-2014 Kurento (http://kurento.org/)
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

var inherits = require('inherits');

var checkType = require('../checkType');


/**
 * Media API for the Kurento Web SDK
 *
 * @module kwsMediaApi/endpoints
 *
 * @copyright 2013-2014 Kurento (http://kurento.org/)
 * @license LGPL
 */

var SessionEndpoint = require('./SessionEndpoint');


/**
 * Implements an SDP negotiation endpoint able to generate and process offers/responses and that configures resources according to negotiated Session Description
 *
 * @abstract
 * @class   module:kwsMediaApi/endpoints~SdpEndpoint
 * @extends module:kwsMediaApi~SessionEndpoint
 */

/**
 * @constructor
 *
 * @param {string} id
 */
function SdpEndpoint(id)
{
  SessionEndpoint.call(this, id);
};
inherits(SdpEndpoint, SessionEndpoint);


/**
 * Request a SessionSpec offer.

   This can be used to initiate a connection.
 *
 * @param {module:kwsMediaApi/endpoints~SdpEndpoint.generateOfferCallback} [callback]
 *
 * @return {module:kwsMediaApi/endpoints~SdpEndpoint}
 *  The own media object
 */
SdpEndpoint.prototype.generateOffer = function(callback){
  return this.invoke('generateOffer', callback);
};
/**
 * @callback SdpEndpoint~generateOfferCallback
 * @param {Error} error
 * @param {String} result
 *  The SDP offer.
 */

/**
 * This method gives access to the SessionSpec offered by this NetworkConnection.

.. note:: This method returns the local MediaSpec, negotiated or not. If no offer has been generated yet, it returns null. It an offer has been generated it returns the offer and if an answer has been processed it returns the negotiated local SessionSpec.
 *
 * @param {module:kwsMediaApi/endpoints~SdpEndpoint.getLocalSessionDescriptorCallback} [callback]
 *
 * @return {module:kwsMediaApi/endpoints~SdpEndpoint}
 *  The own media object
 */
SdpEndpoint.prototype.getLocalSessionDescriptor = function(callback){
  return this.invoke('getLocalSessionDescriptor', callback);
};
/**
 * @callback SdpEndpoint~getLocalSessionDescriptorCallback
 * @param {Error} error
 * @param {String} result
 *  The last agreed SessionSpec
 */

/**
 * This method gives access to the remote session description.

.. note:: This method returns the media previously agreed after a complete offer-answer exchange. If no media has been agreed yet, it returns null.
 *
 * @param {module:kwsMediaApi/endpoints~SdpEndpoint.getRemoteSessionDescriptorCallback} [callback]
 *
 * @return {module:kwsMediaApi/endpoints~SdpEndpoint}
 *  The own media object
 */
SdpEndpoint.prototype.getRemoteSessionDescriptor = function(callback){
  return this.invoke('getRemoteSessionDescriptor', callback);
};
/**
 * @callback SdpEndpoint~getRemoteSessionDescriptorCallback
 * @param {Error} error
 * @param {String} result
 *  The last agreed User Agent session description
 */

/**
 * Request the NetworkConnection to process the given SessionSpec answer (from the remote User Agent).
 *
 * @param {String} answer
 *  SessionSpec answer from the remote User Agent
 *
 * @param {module:kwsMediaApi/endpoints~SdpEndpoint.processAnswerCallback} [callback]
 *
 * @return {module:kwsMediaApi/endpoints~SdpEndpoint}
 *  The own media object
 */
SdpEndpoint.prototype.processAnswer = function(answer, callback){
  checkType('String', 'answer', answer, {required: true});

  var params = {
    answer: answer,
  };

  return this.invoke('processAnswer', params, callback);
};
/**
 * @callback SdpEndpoint~processAnswerCallback
 * @param {Error} error
 * @param {String} result
 *  Updated SDP offer, based on the answer received.
 */

/**
 * Request the NetworkConnection to process the given SessionSpec offer (from the remote User Agent)
 *
 * @param {String} offer
 *  SessionSpec offer from the remote User Agent
 *
 * @param {module:kwsMediaApi/endpoints~SdpEndpoint.processOfferCallback} [callback]
 *
 * @return {module:kwsMediaApi/endpoints~SdpEndpoint}
 *  The own media object
 */
SdpEndpoint.prototype.processOffer = function(offer, callback){
  checkType('String', 'offer', offer, {required: true});

  var params = {
    offer: offer,
  };

  return this.invoke('processOffer', params, callback);
};
/**
 * @callback SdpEndpoint~processOfferCallback
 * @param {Error} error
 * @param {String} result
 *  The chosen configuration from the ones stated in the SDP offer
 */


/**
 * @type module:kwsMediaApi/endpoints~SdpEndpoint.constructorParams
 */
SdpEndpoint.constructorParams = {};

/**
 * @type   module:kwsMediaApi/endpoints~SdpEndpoint.events
 * @extend module:kwsMediaApi~SessionEndpoint.events
 */
SdpEndpoint.events = [];
SdpEndpoint.events.concat(SessionEndpoint.events);


module.exports = SdpEndpoint;
