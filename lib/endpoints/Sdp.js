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
 * @module kwsMediaApi/elements
 *
 * @copyright 2014 Kurento (http://kurento.org/)
 * @license LGPL
 */

var Session = require('./Session');

var extend   = require('extend');
var inherits = require('inherits');

var checkType = require('../checkType');


/**
 * @class
 * @extends module:kwsMediaApi/elements~Session
 *
 * @param id
 * @param {module:kwsMediaApi~MediaContainer} parent
 * @param {module:kwsMediaApi~MediaPipeline} pipeline
 * @param {module:kwsMediaApi/elements~Sdp.paramsScheme} params
 */
function Sdp(id, parent, pipeline, params)
{
  Session.call(this, id, parent, pipeline);
};
inherits(Sdp, Session);


Sdp.prototype.getLocalSessionDescriptor = function(callback)
{
  return this.invoke('getLocalSessionDescriptor', callback);
};

Sdp.prototype.getRemoteSessionDescriptor = function(callback)
{
  return this.invoke('getRemoteSessionDescriptor', callback);
};

Sdp.prototype.generateOffer = function(callback)
{
  return this.invoke('generateOffer', callback);
};

Sdp.prototype.processOffer = function(offer, callback)
{
  checkType('String', 'offer', offer, true);

  return this.invoke('processOffer', {offer: offer}, callback);
};

Sdp.prototype.processAnswer = function(answer, callback)
{
  checkType('String', 'answer', answer, true);

  return this.invoke('processAnswer', {answer: answer}, callback);
};


Sdp.checkparams = function(params)
{
  disconnectionTimeout:
  {
    type: 'Integer'
  }
};
extend(Sdp.paramsScheme, Session.paramsScheme);


module.exports = Sdp;
