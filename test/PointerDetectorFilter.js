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

  kwsMediaApi = require('..');

  require('./_common');
  require('./_proxy');
};


var PlayerEndpoint        = kwsMediaApi.endpoints.PlayerEndpoint;
var PointerDetectorFilter = kwsMediaApi.filters.PointerDetectorFilter;


QUnit.module('PointerDetectorFilter', lifecycle);

QUnit.asyncTest('Detect pointer', function()
{
  QUnit.expect(1);


  var timeoutDelay = 20 * 1000;


  var timeout;

  function _onerror(message)
  {
    clearTimeout(timeout);

    onerror(message);
  };

  function enableTimeout()
  {
    timeout = setTimeout(_onerror, timeoutDelay, 'Time out');
  };

  function disableTimeout()
  {
    clearTimeout(timeout);
  };


  PlayerEndpoint.create(pipeline, {uri: URL_POINTER_DETECTOR}, function(error, player)
  {
    if(error) return onerror(error);

    PointerDetectorFilter.create(pipeline, function(error, pointerDetector)
    {
      if(error) return onerror(error);

      pipeline.connect(player, pointerDetector, function(error, pipeline)
      {
        if(error) return onerror(error);

        player.play(function(error)
        {
          if(error) return onerror(error);

          enableTimeout();
        });
      });

      var window =
      {
        id: 'goal',
        height: 50,
        width: 50,
        upperRightX: 150,
        upperRightY: 150
      };
      pointerDetector.addWindow(window);

      pointerDetector.on('WindowIn', function(data)
      {
        QUnit.ok(true, 'WindowIn');

        disableTimeout();

        QUnit.start();
      });
    });
  });
});

QUnit.test('Window events', function()
{
  QUnit.stop(2);
  QUnit.expect(2);


  var timeoutDelay0 = 10 * 1000;
  var timeoutDelay1 = 20 * 1000;


  PlayerEndpoint.create(pipeline, {uri: URL_POINTER_DETECTOR}, function(error, player)
  {
    if(error) return onerror(error);

    PointerDetectorFilter.create(pipeline, function(error, pointerDetector)
    {
      if(error) return onerror(error);

      var timeout0;
      var timeout1;

      function _onerror(message)
      {
        clearTimeout(timeout0);
        clearTimeout(timeout1);

        onerror(message);
      };

      pipeline.connect(player, pointerDetector, function(error, pipeline)
      {
        if(error) return onerror(error);

        player.play(function(error)
        {
          if(error) return onerror(error);

          timeout0 = setTimeout(_onerror, timeoutDelay0, 'Time out 0');
          timeout1 = setTimeout(_onerror, timeoutDelay1, 'Time out 1');
        });
      });

      var window0 =
      {
        id: 'window0',
        height: 50,
        width: 50,
        upperRightX: 200,
        upperRightY: 50
      };
      var window1 =
      {
        id: 'window1',
        height: 50,
        width: 50,
        upperRightX: 200,
        upperRightY: 150
      };
      pointerDetector.addWindow(window0);
      pointerDetector.addWindow(window1);

      pointerDetector.on('WindowIn', function(data)
      {
        if(data.windowId == 'window0')
        {
          QUnit.ok(true, 'WindowIn');

          clearTimeout(timeout0);

          QUnit.start();
        };
      });

      pointerDetector.on('WindowOut', function(data)
      {
        if(data.windowId == 'window1')
        {
          QUnit.ok(true, 'WindowOut');

          clearTimeout(timeout1);

          QUnit.start();
        };
      });
    });
  });
});

QUnit.test('Window overlay', function()
{
  QUnit.stop(2);
  QUnit.expect(2);


  var timeoutDelay0 = 10 * 1000;
  var timeoutDelay1 = 15 * 1000;


  PlayerEndpoint.create(pipeline, {uri: URL_POINTER_DETECTOR}, function(error, player)
  {
    if(error) return onerror(error);

    PointerDetectorFilter.create(pipeline, function(error, pointerDetector)
    {
      if(error) return onerror(error);

      var timeout0;
      var timeout1;

      function _onerror(message)
      {
        clearTimeout(timeout0);
        clearTimeout(timeout1);

        onerror(message);
      };

      pipeline.connect(player, pointerDetector, function(error, pipeline)
      {
        if(error) return onerror(error);

        player.play(function(error)
        {
          if(error) return onerror(error);

          timeout0 = setTimeout(_onerror, timeoutDelay0, 'Time out WindowIn');
          timeout1 = setTimeout(_onerror, timeoutDelay1, 'Time out WindowOut');
        });
      });

      var window0 =
      {
        id: 'window0',
        height: 50,
        width: 50,
        upperRightX: 200,
        upperRightY: 50
      };
      pointerDetector.addWindow(window0);

      pointerDetector.on('WindowIn', function(data)
      {
        if(data.windowId == 'window0')
        {
          QUnit.ok(true, 'WindowIn');

          clearTimeout(timeout0);

          QUnit.start();
        };
      });

      pointerDetector.on('WindowOut', function(data)
      {
        if(data.windowId == 'window0')
        {
          QUnit.ok(true, 'WindowOut');

          clearTimeout(timeout1);

          QUnit.start();
        };
      });
    });
  });
});
