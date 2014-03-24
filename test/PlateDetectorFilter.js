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

  require('./_common');
  require('./_proxy');
};


var PlayerEndpoint      = KwsMedia.endpoints.PlayerEndpoint;
var PlateDetectorFilter = KwsMedia.filters.PlateDetectorFilter;


QUnit.module('PlateDetectorFilter', lifecycle);

QUnit.asyncTest('Detect plate in a video', function()
{
  QUnit.expect(1);


  var timeoutDelay = 10 * 1000;


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


  PlayerEndpoint.create(pipeline, {uri: URL_PLATES}, function(error, player)
  {
    if(error) return onerror(error);

    PlateDetectorFilter.create(pipeline, function(error, plateDetector)
    {
      if(error) return onerror(error);

      pipeline.connect(player, plateDetector, function(error, pipeline)
      {
        if(error) return onerror(error);

        player.play(function(error)
        {
          if(error) return onerror(error);

          enableTimeout();
        });
      });

      plateDetector.on('PlateDetected', function(data)
      {
        QUnit.ok(true, 'PlateDetected: '+data.plate);

        disableTimeout();

        QUnit.start();
      });
    });
  });
});
