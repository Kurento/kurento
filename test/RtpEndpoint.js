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
 * {@link RtpEndpoint} test suite.
 * 
 * <p>
 * Methods tested:
 * <ul>
 * <li>{@link HttpEndpoint#getUrl()}
 * </ul>
 * <p>
 * Events tested:
 * <ul>
 * <li>{@link HttpEndpoint#addMediaSessionStartListener(MediaEventListener)}
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

  wock = require('wock');

  kwsMediaApi = require('..');

  require('./_common');
  require('./_proxy');
};


QUnit.module('RtpEndpoint', lifecycle);

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
  QUnit.expect(1);

  this.pipeline.create('RtpEndpoint', function(error, rtpEndpoint)
  {
    if(error) return onerror(error);

    rtpEndpoint.generateOffer(function(error)
    {
      if(error) return onerror(error);

      rtpEndpoint.getLocalSessionDescriptor(function(error, sdp)
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
  QUnit.expect(1);

  this.pipeline.create('RtpEndpoint', function(error, rtpEndpoint)
  {
    if(error) return onerror(error);

    rtpEndpoint.processOffer(offer, function(error)
    {
      if(error) return onerror(error);

      rtpEndpoint.getRemoteSessionDescriptor(function(error, sdp)
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

  this.pipeline.create('RtpEndpoint', function(error, rtpEndpoint)
  {
    if(error) return onerror(error);

    rtpEndpoint.generateOffer(function(error, offer)
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

  this.pipeline.create('RtpEndpoint', function(error, rtpEndpoint)
  {
    if(error) return onerror(error);

    rtpEndpoint.processOffer(offer, function(error, answer)
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

  self.pipeline.create('RtpEndpoint', function(error, rtpEndpoint)
  {
    if(error) return onerror(error);

    rtpEndpoint.generateOffer(function(error, offer)
    {
      if(error) return onerror(error);

      QUnit.notEqual(offer, undefined, 'Offer: '+offer);

      self.pipeline.create('RtpEndpoint', function(error, rtpEndpoint2)
      {
        if(error) return onerror(error);

        rtpEndpoint2.processOffer(offer, function(error, answer)
        {
          if(error) return onerror(error);

          QUnit.notEqual(answer, undefined, 'Answer: '+answer);

          rtpEndpoint.processAnswer(answer, function(error, sdp)
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

  QUnit.expect(2);

  self.pipeline.create('PlayerEndpoint', {uri: URL_BARCODES},
  function(error, player)
  {
    if(error) return onerror(error);

    QUnit.notEqual(player, undefined, 'player');

    self.pipeline.create('RtpEndpoint', function(error, rtpEndpoint)
    {
      if(error) return onerror(error);

      QUnit.notEqual(rtpEndpoint, undefined, 'rtpEndpoint');

      player.connect(rtpEndpoint, 'VIDEO', function(error)
      {
        rtpEndpoint.processOffer(offer, function(error)
        {
          if(error) return onerror(error);

          player.play(function(error)
          {
            if(error) return onerror(error);

            setTimeout(QUnit.start.bind(QUnit), 2*1000);
          })
        });
      });
    });
  });
});
