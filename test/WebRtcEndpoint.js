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
 * {@link WebRtcEndpoint} test suite.
 *
 * <p>
 * Methods tested:
 * <ul>
 * <li>{@link WebRtcEndpoint#getLocalSessionDescriptor()}
 * </ul>
 * <p>
 * Events tested:
 * <ul>
 * <li>{@link WebRtcEndpoint#addMediaSessionStartListener(MediaEventListener)}
 * <li>
 * {@link HttpEndpoint#addMediaSessionTerminatedListener(MediaEventListener)}
 * </ul>
 *
 *
 * @author Jesús Leganés Combarro "piranna" (piranna@gmail.com)
 * @since 4.2.4
 *
 */

if(typeof QUnit == 'undefined')
{
  QUnit = require('qunit-cli');
  QUnit.load();

  kurentoClient = require('..');

  require('./_common');
  require('./_proxy');
};


QUnit.module('WebRtcEndpoint', lifecycle);

var offer = "v=0\r\n"
          + "o=- 12345 12345 IN IP4 95.125.31.136\r\n"
          + "s=-\r\n"
          + "c=IN IP4 95.125.31.136\r\n"
          + "t=0 0\r\n"
          + "m=video 52126 RTP/AVP 96 97 98\r\n"
          + "a=rtpmap:96 H264/90000\r\n"
          + "a=rtpmap:97 MP4V-ES/90000\r\n"
          + "a=rtpmap:98 H263-1998/90000\r\n"
          + "a=recvonly\r\n"
          + "b=AS:384\r\n";

QUnit.asyncTest('Get local session descriptor', function()
{
  QUnit.expect(2);

  this.pipeline.create('WebRtcEndpoint', function(error, webRtcEndpoint)
  {
    if(error) return onerror(error);

    webRtcEndpoint.generateOffer(function(error)
    {
      QUnit.equal(error, undefined, 'generateOffer');

      if(error) return onerror(error);

      webRtcEndpoint.getLocalSessionDescriptor(function(error, sdp)
      {
        if(error) return onerror(error);

        QUnit.notEqual(sdp, undefined, 'SDP: '+sdp);

        QUnit.start();
      });
    });
  });
});

QUnit.asyncTest('Get remote session descriptor', function()
{
  QUnit.expect(2);

  this.pipeline.create('WebRtcEndpoint', function(error, webRtcEndpoint)
  {
    if(error) return onerror(error);

    webRtcEndpoint.processOffer(offer, function(error)
    {
      QUnit.equal(error, undefined, 'processOffer');

      if(error) return onerror(error);

      webRtcEndpoint.getRemoteSessionDescriptor(function(error, sdp)
      {
        if(error) return onerror(error);

        QUnit.notEqual(sdp, undefined, 'SDP: '+sdp);

        QUnit.start();
      });
    });
  });
});

QUnit.asyncTest('Generate offer', function()
{
  QUnit.expect(1);

  this.pipeline.create('WebRtcEndpoint', function(error, webRtcEndpoint)
  {
    if(error) return onerror(error);

    webRtcEndpoint.generateOffer(function(error, offer)
    {
      if(error) return onerror(error);

      QUnit.notEqual(offer, undefined, 'Offer: '+offer);

      QUnit.start();
    });
  });
});

QUnit.asyncTest('Process offer', function()
{
  QUnit.expect(1);

  this.pipeline.create('WebRtcEndpoint', function(error, webRtcEndpoint)
  {
    if(error) return onerror(error);

    webRtcEndpoint.processOffer(offer, function(error, answer)
    {
      if(error) return onerror(error);

      QUnit.notEqual(answer, undefined, 'Answer: '+answer);

      QUnit.start();
    });
  });
});

QUnit.asyncTest('Process answer', function()
{
  var self = this;

  QUnit.expect(3);

  self.pipeline.create('WebRtcEndpoint', function(error, webRtcEndpoint)
  {
    if(error) return onerror(error);

    webRtcEndpoint.generateOffer(function(error, offer)
    {
      if(error) return onerror(error);

      QUnit.notEqual(offer, undefined, 'Offer: '+offer);

      self.pipeline.create('WebRtcEndpoint', function(error, webRtcEndpoint2)
      {
        if(error) return onerror(error);

        webRtcEndpoint2.processOffer(offer, function(error, answer)
        {
          if(error) return onerror(error);

          QUnit.notEqual(answer, undefined, 'Answer: '+answer);

          webRtcEndpoint.processAnswer(answer, function(error, sdp)
          {
            if(error) return onerror(error);

            QUnit.notEqual(sdp, undefined, 'SDP: '+sdp);

            QUnit.start();
          });
        });
      });
    });
  });
});

QUnit.asyncTest('RtpEndpoint simulating Android SDP', function()
{
  var self = this;

  QUnit.expect(3);

  self.pipeline.create('PlayerEndpoint', {uri: URL_BARCODES},
  function(error, player)
  {
    if(error) return onerror(error);

    self.pipeline.create('WebRtcEndpoint', function(error, webRtcEndpoint)
    {
      if(error) return onerror(error);

      player.connect(webRtcEndpoint, 'VIDEO', function(error)
      {
        QUnit.equal(error, undefined, 'connect');

        if(error) return onerror(error);

        webRtcEndpoint.processOffer(offer, function(error)
        {
          QUnit.equal(error, undefined, 'processOffer');

          if(error) return onerror(error);

          player.play(function(error)
          {
            QUnit.equal(error, undefined, 'play');

            if(error) return onerror(error);

            setTimeout(QUnit.start.bind(QUnit), 2*1000);
          })
        });
      });
    });
  });
});
