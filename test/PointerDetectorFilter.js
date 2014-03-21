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


var PlayerEndpoint        = KwsMedia.endpoints.PlayerEndpoint;
var PointerDetectorFilter = KwsMedia.filters.PointerDetectorFilter;


QUnit.module('PointerDetectorFilter', lifecycle);

QUnit.asyncTest('Detect pointer', function()
{
  QUnit.expect(1);


  var timeoutDelay = 20 * 1000;


  kwsMedia.on('connect', function()
  {
    kwsMedia.createMediaPipeline(function(error, pipeline)
    {
      if(error) return onerror(error);

      PlayerEndpoint.create(pipeline, {uri: URL_PLATES},
      function(error, player)
      {
        if(error) return onerror(error);

        PointerDetectorFilter.create(pipeline, function(error, pointerDetector)
        {
          if(error) return onerror(error);

          var timeout;

          pipeline.connect(player, pointerDetector, function(error, pipeline)
          {
            if(error) return onerror(error);

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

          pointerDetector.addWindow('goal', 50, 50, 150, 150);

          pointerDetector.on('WindowIn', function(data)
          {
            QUnit.ok(true, 'WindowIn');

            clearTimeout(timeout);

            QUnit.start();
          });
        });
      });
    })
  });
});

QUnit.asyncTest('Window events', function()
{
  QUnit.expect(2);


  var timeoutDelay0 = 20 * 1000;
  var timeoutDelay1 =  5 * 1000;


  kwsMedia.on('connect', function()
  {
    kwsMedia.createMediaPipeline(function(error, pipeline)
    {
      if(error) return onerror(error);

      PlayerEndpoint.create(pipeline, {uri: URL_PLATES},
      function(error, player)
      {
        if(error) return onerror(error);

        PointerDetectorFilter.create(pipeline, function(error, pointerDetector)
        {
          if(error) return onerror(error);

          var timeout0;
          var timeout1;

          pipeline.connect(player, pointerDetector, function(error, pipeline)
          {
            if(error) return onerror(error);

            player.play(function(error)
            {
              if(error) return onerror(error);

              timeout0 = setTimeout(function()
              {
                onerror('Time out 0');
              },
              timeoutDelay0);

              timeout1 = setTimeout(function()
              {
                onerror('Time out 1');
              },
              timeoutDelay1);
            });
          });

          pointerDetector.addWindow('window0', 50, 50, 200,  50);
          pointerDetector.addWindow('window1', 50, 50, 200, 150);

          pointerDetector.on('WindowIn', function(data)
          {
            QUnit.ok(true, 'WindowIn');

            clearTimeout(timeout0);

            QUnit.start();
          });

          pointerDetector.on('WindowOut', function(data)
          {
            QUnit.ok(true, 'WindowOut');

            clearTimeout(timeout1);

            QUnit.start();
          });
        });
      });
    })
  });
});

QUnit.asyncTest('Window overlay', function()
{
  QUnit.expect(2);


  var timeoutDelay0 = 10 * 1000;
  var timeoutDelay1 =  5 * 1000;


  kwsMedia.on('connect', function()
  {
    kwsMedia.createMediaPipeline(function(error, pipeline)
    {
      if(error) return onerror(error);

      PlayerEndpoint.create(pipeline, {uri: URL_PLATES},
      function(error, player)
      {
        if(error) return onerror(error);

        PointerDetectorFilter.create(pipeline, function(error, pointerDetector)
        {
          if(error) return onerror(error);

          var timeout0;
          var timeout1;

          pipeline.connect(player, pointerDetector, function(error, pipeline)
          {
            if(error) return onerror(error);

            player.play(function(error)
            {
              if(error) return onerror(error);

              timeout0 = setTimeout(function()
              {
                onerror('Time out WindowIn');
              },
              timeoutDelay0);

              timeout1 = setTimeout(function()
              {
                onerror('Time out WindowOut');
              },
              timeoutDelay1);
            });
          });

          pointerDetector.addWindow('window0', 50, 50, 200, 50);

          pointerDetector.on('WindowIn', function(data)
          {
            QUnit.ok(true, 'WindowIn');

            clearTimeout(timeout0);

            QUnit.start();
          });

          pointerDetector.on('WindowOut', function(data)
          {
            QUnit.ok(true, 'WindowOut');

            clearTimeout(timeout1);

            QUnit.start();
          });
        });
      });
    })
  });
});
