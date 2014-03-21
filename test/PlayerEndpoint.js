/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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

  KwsMedia = require('..');
};


var PlayerEndpoint = KwsMedia.endpoints.PlayerEndpoint;


QUnit.module('PlayerEndpoint', lifecycle);

QUnit.asyncTest('Play, Pause & Stop', function()
{
  QUnit.expect(5);

  kwsMedia.on('connect', function()
  {
    kwsMedia.createMediaPipeline(function(error, pipeline)
    {
      if(error) return onerror(error);

      QUnit.notEqual(pipeline, undefined, 'pipeline');

      PlayerEndpoint.create(pipeline, {uri: URL_SMALL},
      function(error, player)
      {
        if(error) return onerror(error);

        QUnit.notEqual(player, undefined, 'player');

        player.play(function(error)
        {
          if(error) return onerror(error);

          QUnit.notEqual(player, undefined, 'play');

          player.pause(function(error)
          {
            if(error) return onerror(error);

            QUnit.notEqual(player, undefined, 'pause');

            player.stop(function(error)
            {
              if(error) return onerror(error);

              QUnit.ok(true, 'stop');

              QUnit.start();
            });
          });
        });
      });
    })
  });
});

QUnit.asyncTest('End of Stream', function()
{
  QUnit.expect(1);


  var timeoutDelay = 5 * 1000;


  kwsMedia.on('connect', function()
  {
    kwsMedia.createMediaPipeline(function(error, pipeline)
    {
      if(error) return onerror(error);

      PlayerEndpoint.create(pipeline, {uri: URL_SMALL},
      function(error, player)
      {
        if(error) return onerror(error);

        var timeout;

        player.on('EndOfStream', function(data)
        {
          QUnit.ok(true, 'EndOfStream');

          clearTimeout(timeout);

          QUnit.start();
        });

        player.play(function(error)
        {
          if(error) return onerror(error);

          timeout = setTimeout(function()
          {
            onerror('Time out');
          },
          timeoutDelay);
        });
      });
    })
  });
});
