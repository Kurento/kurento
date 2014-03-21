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


var PlayerEndpoint  = KwsMedia.endpoints.PlayerEndpoint;
var HttpGetEndpoint = KwsMedia.endpoints.HttpGetEndpoint;


function doGet(url, onsuccess, onerror)
{
  // Node.js
  if(typeof XMLHttpRequest == 'undefined')
    require('http').get(url, onsuccess).on('error', onerror);

  // browser
  else
  {
    var xhr = new XMLHttpRequest();

    xhr.open("get", url);
    xhr.send();

    xhr.addEventListener('load', onsuccess);
    xhr.addEventListener('error', onerror);
  };
};


QUnit.module('HttpGetEndpoint', lifecycle);

QUnit.asyncTest('Method GetUrl', function()
{
  QUnit.expect(3);

  kwsMedia.on('connect', function()
  {
    kwsMedia.createMediaPipeline(function(error, pipeline)
    {
      if(error) return onerror(error);

      QUnit.notEqual(pipeline, undefined, 'pipeline');

      HttpGetEndpoint.create(pipeline, function(error, httpGet)
      {
        if(error) return onerror(error);

        QUnit.notEqual(httpGet, undefined, 'httpGet');

        httpGet.getUrl(function(error, url)
        {
          if(error) return onerror(error);

          QUnit.notEqual(url, undefined, 'URL: '+url);

          QUnit.start();
        })
      });
    })
  });
});

QUnit.asyncTest('Media session started', function()
{
  QUnit.expect(6);


  var timeoutDelay = 7 * 1000;


  var timeout;

  function enableTimeout()
  {
    timeout = setTimeout(function()
    {
      onerror('Time out');
    },
    timeoutDelay);
  };

  function disableTimeout()
  {
    clearTimeout(timeout);
  };


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

        player.on('EndOfStream', function(data)
        {
          QUnit.ok(true, 'EndOfStream');

          disableTimeout();

          QUnit.start();
        });

        HttpGetEndpoint.create(pipeline, function(error, httpGet)
        {
          if(error) return onerror(error);

          QUnit.notEqual(httpGet, undefined, 'httpGet');

          httpGet.on('MediaSessionStarted', function(data)
          {
            QUnit.ok(true, 'MediaSessionStarted');

            disableTimeout();

            player.play(function(error)
            {
              if(error) return onerror(error);

              enableTimeout();
            });
          });

          player.connect(httpGet, function(error)
          {
            if(error) return onerror(error);

            httpGet.getUrl(function(error, url)
            {
              if(error) return onerror(error);

              QUnit.notEqual(url, undefined, 'URL: '+url);

              // This should trigger MediaSessionStarted event
              doGet(url, enableTimeout, onerror);
            })
          });
        });
      });
    })
  });
});
