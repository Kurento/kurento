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
 * @module kwsMediaApi/hubs
 *
 * @copyright 2013-2014 Kurento (http://kurento.org/)
 * @license LGPL
 */

var Hub = require('../core/Hub');


/**
 * A :rom:cls:`Hub` that allows routing of video between arbitrary port pairs and mixing of audio among several ports
 *
 * @class   module:kwsMediaApi/hubs~Mixer
 * @extends module:kwsMediaApi~Hub
 */

/**
 * Create a :rom:cls:`Mixer` belonging to the given pipeline.
 *
 * @constructor
 *
 * @param {string} id
 */
function Mixer(id)
{
  Hub.call(this, id);
};
inherits(Mixer, Hub);


/**
 * Connects each corresponding :rom:enum:`MediaType` of the given source port with the sink port.
 *
 * @param {MediaType} media
 *  The sort of media stream to be connected
 *
 * @param {HubPort} source
 *  Video source port to be connected
 *
 * @param {HubPort} sink
 *  Video sink port to be connected
 *
 * @param {module:kwsMediaApi/hubs~Mixer.connectCallback} [callback]
 *
 * @return {module:kwsMediaApi/hubs~Mixer}
 *  The own media object
 */
Mixer.prototype.connect = function(media, source, sink, callback){
  checkType('MediaType', 'media', media, {required: true});
  checkType('HubPort', 'source', source, {required: true});
  checkType('HubPort', 'sink', sink, {required: true});

  var params = {
    media: media,
    source: source,
    sink: sink,
  };

  return this.invoke('connect', params, callback);
};
/**
 * @callback Mixer~connectCallback
 * @param {Error} error
 */

/**
 * Disonnects each corresponding :rom:enum:`MediaType` of the given source port from the sink port.
 *
 * @param {MediaType} media
 *  The sort of media stream to be disconnected
 *
 * @param {HubPort} source
 *  Audio source port to be disconnected
 *
 * @param {HubPort} sink
 *  Audio sink port to be disconnected
 *
 * @param {module:kwsMediaApi/hubs~Mixer.disconnectCallback} [callback]
 *
 * @return {module:kwsMediaApi/hubs~Mixer}
 *  The own media object
 */
Mixer.prototype.disconnect = function(media, source, sink, callback){
  checkType('MediaType', 'media', media, {required: true});
  checkType('HubPort', 'source', source, {required: true});
  checkType('HubPort', 'sink', sink, {required: true});

  var params = {
    media: media,
    source: source,
    sink: sink,
  };

  return this.invoke('disconnect', params, callback);
};
/**
 * @callback Mixer~disconnectCallback
 * @param {Error} error
 */


/**
 * @type module:kwsMediaApi/hubs~Mixer.constructorParams
 *
 * @property {MediaPipeline} mediaPipeline
 *  the :rom:cls:`MediaPipeline` to which the Mixer belongs
 */
Mixer.constructorParams = {
  mediaPipeline: {
    type: 'MediaPipeline',
    required: true
  },
};

/**
 * @type   module:kwsMediaApi/hubs~Mixer.events
 * @extend module:kwsMediaApi~Hub.events
 */
Mixer.events = [];
Mixer.events.concat(Hub.events);


module.exports = Mixer;


Mixer.check = function(key, value)
{
  if(!(value instanceof Mixer))
    throw SyntaxError(key+' param should be a Mixer, not '+typeof value);
};
