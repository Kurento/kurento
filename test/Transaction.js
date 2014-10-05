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
 * {@link MediaPipeline} basic test suite.
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


QUnit.module('Transaction', lifecycle);

QUnit.asyncTest('transaction', function()
{
  var self = this;

  QUnit.expect(3);

  // Pipeline creation (implicit transaction)
  var pipeline = self.pipeline;

  var player  = pipeline.create('PlayerEndpoint', {uri: URL_SMALL});
  var httpGet = pipeline.create('HttpGetEndpoint');

  player.connect(httpGet);

  // End pipeline creation
  pipeline.start(function(error)
  {
    if(error) return onerror(error);

    var url1;
    // Atomic operation
    httpGet.getUrl(function(url)
    {
      url1 = url;
    });
    // End atomic operation

    // Explicit transaction
		pipeline.transaction(function()
    {
      player.play();

      httpGet.getUrl(function(url2)
      {
        QUnit.equal(url2, url1, 'URL: '+url2);

        QUnit.start();
      });

      pipeline.release();
    });
		// End explicit transaction
  });
});

QUnit.asyncTest('transaction creation', function()
{
  var self = this;

  QUnit.expect(3);

  // Pipeline creation (implicit transaction)
  var pipeline = self.pipeline;
  pipeline.start(function(error)
  {
    if(error) return onerror(error);

    // Atomic creation
    var player = pipeline.create('PlayerEndpoint', {uri: URL_SMALL});
    // End atomic creation

    // Creation in explicit transaction
    pipeline.transaction(function()
    {
      var httpGet = pipeline.create('HttpGetEndpoint');

      player.connect(httpGet);
    },
    // End transaction
    function()
    {
      httpGet.getUrl(function(url)
      {
        QUnit.notEqual(url, undefined, 'URL: '+url);

        QUnit.start();
      });
    });
  });
});

QUnit.asyncTest('use plain methods in new objects inside transaction', function()
{
  var self = this;

  QUnit.expect(3);

  // Pipeline creation (implicit transaction)
  var pipeline = self.pipeline;

  var player = pipeline.create('PlayerEndpoint', {uri: URL_SMALL});

  pipeline.start(function(error)
  {
    if(error) return onerror(error);

    // Creation in explicit transaction
    pipeline.transaction(function()
    {
      var httpGet = pipeline.create('HttpGetEndpoint');

      player.connect(httpGet);
    },
    // End transaction
    function()
    {
      QUnit.start();
    });
  });
});

QUnit.asyncTest('use creation transaction', function()
{
  var self = this;

  QUnit.expect(3);

  // Pipeline creation (implicit transaction)
  var pipeline = self.pipeline;

  var player  = pipeline.create('PlayerEndpoint', {uri: URL_SMALL});
  var httpGet = pipeline.create('HttpGetEndpoint');

  player.connect(httpGet);

  var url;

  httpGet.geturl(function(url2)
  {
    url = url2;
  })

  pipeline.start(function(error)
  {
    if(error) return onerror(error);

    QUnit.notEqual(url, undefined, 'URL: '+url);

    QUnit.start();
  });
});

QUnit.asyncTest('use thenable on element', function()
{
  var self = this;

  QUnit.expect(3);

  // Pipeline creation (implicit transaction)
  var pipeline = self.pipeline;

  var player  = pipeline.create('PlayerEndpoint', {uri: URL_SMALL});
  var httpGet = pipeline.create('HttpGetEndpoint');

  player.connect(httpGet);

  httpGet.then(function()
  {
    this.getUrl(function(url)
    {
      QUnit.notEqual(url, undefined, 'URL: '+url);

      QUnit.start();
    });
  })

  pipeline.start();
});

QUnit.asyncTest('isReady', function()
{
  var self = this;

  QUnit.expect(2);

  // Pipeline creation (implicit transaction)
  var pipeline = self.pipeline;

  var player  = pipeline.create('PlayerEndpoint', {uri: URL_SMALL});
  var httpGet = pipeline.create('HttpGetEndpoint');

  player.connect(httpGet);

  QUnit.notOk(player.isReady());

  pipeline.start(function()
  {
    QUnit.ok(player.isReady());

    QUnit.start();
  });
});

QUnit.asyncTest('wait ready', function()
{
  var self = this;

  QUnit.expect(2);

  // Pipeline creation (implicit transaction)
  var pipeline = self.pipeline;

  var player  = pipeline.create('PlayerEndpoint', {uri: URL_SMALL});
  var httpGet = pipeline.create('HttpGetEndpoint');

  player.connect(httpGet);

  player.then(function()
  {
    QUnit.ok(player.isReady());

    QUnit.start();
  })

  pipeline.start();
});

QUnit.asyncTest('future', function()
{
  var self = this;

  QUnit.expect(2);

  // Pipeline creation (implicit transaction)
  var pipeline = self.pipeline;

  var player  = pipeline.create('PlayerEndpoint', {uri: URL_SMALL});
  var httpGet = pipeline.create('HttpGetEndpoint');

  player.connect(httpGet);

  pipeline.start(function()
  {
    // Atomic operation
    var url = httpGet.getUrl(function(url)
    {
      httpGet.getMediaPipeline(function(rPipeline)
      {
        player.getUri(function(uri)
        {
          // Explicit transaction
          pipeline.transaction(function(ctx)
          {
            ctx.fUrl = httpGet.getUrl();
            ctx.fUri = player.getUri();
          },
          // End explicit transaction
          function(ctx)
          {
            QUnit.equal(ctx.fUrl, url);
            QUnit.equal(ctx.fUri, uri);
          })
        });
        // End atomic operation
      });
    });
  });

  player.then(function()
  {
    QUnit.ok(player.isReady());

    QUnit.start();
  })
});
