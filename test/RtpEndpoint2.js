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
 * {@link RtpEndpoint2} test suite.
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

  kurentoClient = require('..');

  require('./_common');
  require('./_proxy');
};


QUnit.module('RtpEndpoint2', lifecycle);

QUnit.asyncTest('CampusParty simulated pipeline', function()
{
  var self = this;

  QUnit.expect(2);

  self.pipeline.create('RtpEndpoint', function(error, rtpEndpoint)
  {
    if(error) return onerror(error);

    var offer = "v=0\r\n"
              + "o=- 12345 12345 IN IP4 192.168.1.18\r\n"
              + "s=-\r\n"
              + "c=IN IP4 192.168.1.18\r\n"
              + "t=0 0\r\n"
              + "m=video 45936 RTP/AVP 96\r\n"
              + "a=rtpmap:96 H263-1998/90000\r\n"
              + "a=sendrecv\r\n"
              + "b=AS:3000\r\n";

    rtpEndpoint.processOffer(offer, function(error)
    {
      if(error) return onerror(error);

      rtpEndpoint.getMediaSrcs('VIDEO', function(error, mediaSources)
      {
        if(error) return onerror(error);

        QUnit.notEqual(mediaSources, [], 'MediaSources: '+mediaSources);

        var mediaSource = mediaSources[0];

        rtpEndpoint.getMediaSinks('VIDEO', function(error, mediaSinks)
        {
          if(error) return onerror(error);

          QUnit.notEqual(mediaSinks, [], 'MediaSinks: '+mediaSinks);

          var mediaSink = mediaSinks[0];

          mediaSource.connect(mediaSink, function(error)
          {
            if(error) return onerror(error);

            self.pipeline.create('HttpGetEndpoint',
            function(error, httpGetEndpoint)
            {
              if(error) return onerror(error);

              rtpEndpoint.connect(httpGetEndpoint, 'VIDEO', function(error)
              {
                if(error) return onerror(error);

                QUnit.start();
              });
            });
          });
        });
      });
    });
  });
});

QUnit.asyncTest('Source sinks', function()
{
  QUnit.expect(4);

  this.pipeline.create('RtpEndpoint', function(error, rtpEndpoint)
  {
    if(error) return onerror(error);

    rtpEndpoint.getMediaSrcs('VIDEO', function(error, mediaSources)
    {
      if(error) return onerror(error);

      QUnit.notEqual(mediaSources, [], 'MediaSources video: '+mediaSources);

      rtpEndpoint.getMediaSinks('VIDEO', function(error, mediaSinks)
      {
        if(error) return onerror(error);

        QUnit.notEqual(mediaSinks, [], 'MediaSinks video: '+mediaSinks);

        rtpEndpoint.getMediaSrcs('AUDIO', function(error, mediaSources)
        {
          if(error) return onerror(error);

          QUnit.notEqual(mediaSources, [], 'MediaSources audio: '+mediaSources);

          rtpEndpoint.getMediaSinks('AUDIO', function(error, mediaSinks)
          {
            if(error) return onerror(error);

            QUnit.notEqual(mediaSinks, [], 'MediaSinks audio: '+mediaSinks);

            rtpEndpoint.release(function(error)
            {
              if(error) return onerror(error);

              QUnit.start();
            });
          });
        });
      });
    });
  });
});

QUnit.asyncTest('Connect', function()
{
  var self = this;

  QUnit.expect(0);

  self.pipeline.create('PlayerEndpoint', {uri: URL_SMALL}, function(error, player)
  {
    if(error) return onerror(error);

    self.pipeline.create('HttpGetEndpoint', function(error, httpGet)
    {
      if(error) return onerror(error);

      player.connect(httpGet, function(error)
      {
        if(error) return onerror(error);

        player.play(function(error)
        {
          if(error) return onerror(error);

          httpGet.release(function(error)
          {
            if(error) return onerror(error);

            player.release(function(error)
            {
              if(error) return onerror(error);

              QUnit.start();
            });
          });
        });
      });
    });
  });
});

QUnit.asyncTest('Connect by type', function()
{
  var self = this;

  QUnit.expect(0);

  self.pipeline.create('PlayerEndpoint', {uri: URL_SMALL}, function(error, player)
  {
    if(error) return onerror(error);

    self.pipeline.create('HttpGetEndpoint', function(error, httpGet)
    {
      if(error) return onerror(error);

      player.connect(httpGet, 'AUDIO', function(error)
      {
        if(error) return onerror(error);

        player.connect(httpGet, 'VIDEO', function(error)
        {
          if(error) return onerror(error);

          player.play(function(error)
          {
            if(error) return onerror(error);

            httpGet.release(function(error)
            {
              if(error) return onerror(error);

              player.release(function(error)
              {
                if(error) return onerror(error);

                QUnit.start();
              });
            });
          });
        });
      });
    });
  });
});
