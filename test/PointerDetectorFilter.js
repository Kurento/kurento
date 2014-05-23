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

  KwsMedia = require('..');

  require('./_common');
  require('./_proxy');
};


QUnit.module('PointerDetectorFilter', lifecycle);

QUnit.asyncTest('Detect pointer', function()
{
  var self = this;

  QUnit.expect(1);

  var timeout = new Timeout('"PointerDetectorFilter:Detect pointer"',
                            20 * 1000, onerror);


  self.pipeline.create('PlayerEndpoint', {uri: URL_POINTER_DETECTOR}, function(error, player)
  {
    if(error) return onerror(error);

    self.pipeline.create('PointerDetectorFilter', function(error, pointerDetector)
    {
      if(error) return onerror(error);

      player.connect(pointerDetector, function(error)
      {
        if(error) return onerror(error);

        player.play(function(error)
        {
          if(error) return onerror(error);

          timeout.start();
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

        timeout.stop();

        QUnit.start();
      });
    });
  });
});

QUnit.test('Window events', function()
{
  var self = this;

  if(!self.pipeline) return start();

  QUnit.stop(2);
  QUnit.expect(2);


  var delay =
  {
    WindowIn:  10 * 1000,
    WindowOut: 20 * 1000
  };

  var timeout = {};

  function _ontimeout(message)
  {
    for(var id in timeout)
      clearTimeout(timeout[id]);

    onerror(message);
  };

  timeout_start = function(id)
  {
    timeout[id] = setTimeout(_ontimeout, delay[id], 'Time out '+id+' ('+delay[id]+'ms)');
  };

  timeout_stop = function(id)
  {
    clearTimeout(timeout[id]);
  };


  self.pipeline.create('PlayerEndpoint', {uri: URL_POINTER_DETECTOR}, function(error, player)
  {
    if(error) return onerror(error);

    self.pipeline.create('PointerDetectorFilter', function(error, pointerDetector)
    {
      if(error) return onerror(error);

      player.connect(pointerDetector, function(error)
      {
        if(error) return onerror(error);

        player.play(function(error)
        {
          if(error) return onerror(error);

          timeout_start('WindowIn');
          timeout_start('WindowOut');
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

      var windowIn_asserted = windowOut_asserted = false;

      pointerDetector.on('WindowIn', function(data)
      {
        if(data.windowId == 'window0' && !windowIn_asserted)
        {
          QUnit.ok(true, 'WindowIn');
          windowIn_asserted = true;

          timeout_stop('WindowIn');

          QUnit.start();
        };
      });

      pointerDetector.on('WindowOut', function(data)
      {
        if(data.windowId == 'window1' && !windowOut_asserted)
        {
          QUnit.ok(true, 'WindowOut');
          windowOut_asserted = true;

          timeout_stop('WindowOut');

          QUnit.start();
        };
      });
    });
  });
});

QUnit.test('Window overlay', function()
{
  var self = this;

  if(!self.pipeline) return start();

  QUnit.stop(2);
  QUnit.expect(2);


  var delay =
  {
    WindowIn:  10 * 1000,
    WindowOut: 15 * 1000
  };

  var timeout = {};

  function _ontimeout(message)
  {
    for(var id in timeout)
      clearTimeout(timeout[id]);

    onerror(message);
  };

  timeout_start = function(id)
  {
    timeout[id] = setTimeout(_ontimeout, delay[id], 'Time out '+id+' ('+delay[id]+'ms)');
  };

  timeout_stop = function(id)
  {
    clearTimeout(timeout[id]);
  };


  self.pipeline.create('PlayerEndpoint', {uri: URL_POINTER_DETECTOR}, function(error, player)
  {
    if(error) return onerror(error);

    self.pipeline.create('PointerDetectorFilter', function(error, pointerDetector)
    {
      if(error) return onerror(error);

      player.connect(pointerDetector, function(error)
      {
        if(error) return onerror(error);

        player.play(function(error)
        {
          if(error) return onerror(error);

          timeout_start('WindowIn');
          timeout_start('WindowOut');
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

          timeout_stop('WindowIn');

          QUnit.start();
        };
      });

      pointerDetector.on('WindowOut', function(data)
      {
        if(data.windowId == 'window0')
        {
          QUnit.ok(true, 'WindowOut');

          timeout_stop('WindowOut');

          QUnit.start();
        };
      });
    });
  });
});
