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

/**
 * {@link HttpEndpoint} test suite.
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
 * @version 1.0.0
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


QUnit.module('PlayerEndpoint', lifecycle);

QUnit.asyncTest('Play, Pause & Stop', function()
{
  var self = this;

  QUnit.expect(4);

  self.pipeline.create('PlayerEndpoint', {uri: URL_SMALL},
  function(error, player)
  {
    if(error) return onerror(error);

    QUnit.notEqual(player, undefined, 'player');

    player.play(function(error)
    {
      QUnit.equal(error, undefined, 'playing');

      if(error) return onerror(error);

      player.pause(function(error)
      {
        QUnit.equal(error, undefined, 'paused');

        if(error) return onerror(error);

        player.stop(function(error)
        {
          QUnit.equal(error, undefined, 'stoped');

          if(error) return onerror(error);

          QUnit.start();
        });
      });
    });
  });
});

QUnit.asyncTest('End of Stream', function()
{
  var self = this;

  QUnit.expect(2);

  var timeout = new Timeout('"PlayerEndpoint:End of Stream"',
                            10 * 1000, onerror);

  function onerror(error)
  {
    timeout.stop();
    _onerror(error);
  };


  self.pipeline.create('PlayerEndpoint', {uri: URL_SMALL}, function(error, player)
  {
    if(error) return onerror(error);

    player.on('EndOfStream', function(data)
    {
      QUnit.ok(true, 'EndOfStream');

      timeout.stop();

      QUnit.start();
    });

    player.play(function(error)
    {
      QUnit.equal(error, undefined, 'playing');

      if(error) return onerror(error);

      timeout.start();
    });
  });
});

QUnit.asyncTest('GetUri', function()
{
  var self = this;

  QUnit.expect(1);

  self.pipeline.create('PlayerEndpoint', {uri: URL_SMALL}, function(error, player)
  {
    if(error) return onerror(error);

    player.getUri(function(error, url)
    {
      if(error) return onerror(error);

      QUnit.equal(url, URL_SMALL, 'URL: '+url);

      QUnit.start();
    });
  });
});


QUnit.asyncTest('Connect', function()
{
  var self = this;

  QUnit.expect(4);

  self.pipeline.create('PlayerEndpoint', {uri: URL_SMALL}, function(error, player)
  {
    if(error) return onerror(error);

    self.pipeline.create('HttpGetEndpoint', function(error, httpGet)
    {
      if(error) return onerror(error);

      player.connect(httpGet, function(error)
      {
        QUnit.equal(error, undefined, 'connect');

        if(error) return onerror(error);

        player.play(function(error)
        {
          QUnit.equal(error, undefined, 'playing');

          if(error) return onerror(error);

          httpGet.release(function(error)
          {
            QUnit.equal(error, undefined, 'release httpGet');

            if(error) return onerror(error);

            player.release(function(error)
            {
              QUnit.equal(error, undefined, 'release player');

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

  QUnit.expect(5);

  self.pipeline.create('PlayerEndpoint', {uri: URL_SMALL}, function(error, player)
  {
    if(error) return onerror(error);

    self.pipeline.create('HttpGetEndpoint', function(error, httpGet)
    {
      if(error) return onerror(error);

      player.connect(httpGet, 'AUDIO', function(error)
      {
        QUnit.equal(error, undefined, 'connect AUDIO');

        if(error) return onerror(error);

        player.connect(httpGet, 'VIDEO', function(error)
        {
          QUnit.equal(error, undefined, 'connect VIDEO');

          if(error) return onerror(error);

          player.play(function(error)
          {
            QUnit.equal(error, undefined, 'play');

            if(error) return onerror(error);

            httpGet.release(function(error)
            {
              QUnit.equal(error, undefined, 'release httpGet');

              if(error) return onerror(error);

              player.release(function(error)
              {
                QUnit.equal(error, undefined, 'release player');

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
