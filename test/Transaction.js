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

  QUnit.expect(1);

  var pipeline = self.pipeline;

  var player  = pipeline.create('PlayerEndpoint', {uri: URL_SMALL});
  var httpGet = pipeline.create('HttpGetEndpoint');

  player.connect(httpGet);

  // Atomic operation
  httpGet.getUrl(function(error, url)
  {
    if(error) return onerror(error);

    var url_preTransaction = url;

    pipeline.transaction(function()
    {
      player.play();

      httpGet.getUrl(function(error, url)
      {
        if(error) return onerror(error);

        QUnit.equal(url, url_preTransaction, 'URL: '+url);

        QUnit.start();
      });
    });
  });
  // End atomic operation
});

QUnit.asyncTest('Transaction object on pseudo-sync API', function()
{
  var self = this;

  QUnit.expect(4);

  var pipeline = self.pipeline;

  var t = pipeline.beginTransaction();

    var player  = pipeline.create(t, 'PlayerEndpoint', {uri: URL_SMALL});
    var httpGet = pipeline.create(t, 'HttpGetEndpoint');

    player.connect(t, httpGet);

    var promiseUrl = httpGet.getUrl(t);

    player.play(t);

    QUnit.strictEqual(player.id, undefined);

  t.endTransaction(function(error)
  {
    QUnit.equal(error, undefined, 'endTransaction');

    QUnit.notStrictEqual(player.id, undefined, 'player.id: '+player.id);

    promiseUrl.then(function(value){
      QUnit.notStrictEqual(value, undefined, 'httpGet.url: '+url);
    });

    pipeline.release(function(error)
    {
      QUnit.equal(error, undefined, 'release');

      if(error) return onerror(error);

      QUnit.start();
    });
  });
});

QUnit.asyncTest('Transaction object on async API', function()
{
  var self = this;

  QUnit.expect(4);

  var pipeline = self.pipeline;

  pipeline.create('PlayerEndpoint', {uri: URL_SMALL}, function(error, player)
  {
    if(error) return onerror(error);

    pipeline.create('HttpGetEndpoint', function(error, httpGet)
    {
      if(error) return onerror(error);

      player.connect(httpGet, function(error)
      {
        QUnit.equal(error, undefined, 'connect');

        if(error) return onerror(error);

        httpGet.getUrl(function(error, url)
        {
          if(error) return onerror(error);

          var t = pipeline.beginTransaction();

            player.play(t);

            var promiseUrl = httpGet.getUrl(t);

          t.endTransaction(function(error)
          {
            QUnit.equal(error, undefined, 'endTransaction');

            if(error) return onerror(error);

            promiseUrl.then(function(value)
            {
              QUnit.equal(value, url, 'URL: '+value);
            })

            pipeline.release(function(error)
            {
              QUnit.equal(error, undefined, 'release');

              if(error) return onerror(error);

              QUnit.start();
            });
          });
        });
      });
    });
  });
});


QUnit.asyncTest('transaction creation', function()
{
  var self = this;

  QUnit.expect(2);

  // Pipeline creation
  var pipeline = self.pipeline;

  // Atomic creation
  var player = pipeline.create('PlayerEndpoint', {uri: URL_SMALL});
  // End atomic creation

  // Creation in transaction
  var httpGet;
  pipeline.transaction(function()
  {
    httpGet = pipeline.create('HttpGetEndpoint');

    player.connect(httpGet);
  },
  // End transaction
  function(error)
  {
    QUnit.equal(error, undefined, 'transaction');

    if(error) return onerror(error);

    httpGet.getUrl(function(error, url)
    {
      if(error) return onerror(error);

      QUnit.notEqual(url, undefined, 'URL: '+url);

      QUnit.start();
    });
  });
});

QUnit.asyncTest('use thenable on element', function()
{
  var self = this;

  QUnit.expect(1);

  // Pipeline creation (implicit transaction)
  var pipeline = self.pipeline;

  var player  = pipeline.create('PlayerEndpoint', {uri: URL_SMALL});
  var httpGet = pipeline.create('HttpGetEndpoint');

  player.connect(httpGet);

  httpGet.then(function()
  {
    this.getUrl(function(error, url)
    {
      if(error) return onerror(error);

      QUnit.notEqual(url, undefined, 'URL: '+url);

      QUnit.start();
    });
  })
});

QUnit.asyncTest('Use thenable on transaction', function()
{
  var self = this;

  QUnit.expect(0);

  // Pipeline creation
  var pipeline = self.pipeline;

  // Atomic creation
  var player = pipeline.create('PlayerEndpoint', {uri: URL_SMALL});
  // End atomic creation

  // Creation in transaction
  var promise = pipeline.transaction(function()
  {
    var httpGet = pipeline.create('HttpGetEndpoint');

    player.connect(httpGet);
  });
  // End transaction

  promise.then(function()
  {
    player.release();

    QUnit.start();
  });
});

QUnit.asyncTest('Is ready after thenable is called', function()
{
  var self = this;

  QUnit.expect(2);

  var pipeline = self.pipeline;

  var player  = pipeline.create('PlayerEndpoint', {uri: URL_SMALL});
  var httpGet = pipeline.create('HttpGetEndpoint');

  player.connect(httpGet);

  QUnit.strictEqual(player.id, undefined);

  player.then(function()
  {
    QUnit.notStrictEqual(player.id, undefined, 'player.id: '+player.id);

    QUnit.start();
  });
});

QUnit.asyncTest('Promise', function()
{
  var self = this;

  QUnit.expect(4);

  // Pipeline creation (implicit transaction)
  var pipeline = self.pipeline;

  var player  = pipeline.create('PlayerEndpoint', {uri: URL_SMALL});
  var httpGet = pipeline.create('HttpGetEndpoint');

  player.connect(httpGet);

  // Atomic operation
  httpGet.getUrl(function(error, url)
  {
    if(error) return onerror(error);

    httpGet.getMediaPipeline(function(error, rPipeline)
    {
      if(error) return onerror(error);

      player.getUri(function(error, uri)
      {
        if(error) return onerror(error);

        QUnit.equal(rPipeline, pipeline);

        // Explicit transaction
        pipeline.transaction(function()
        {
          httpGet.getUrl(function(error, fUrl)
          {
            QUnit.equal(fUrl, url, 'URL: '+fUrl);
          });
          player.getUri(function(error, fUri)
          {
            QUnit.equal(fUri, uri, 'URI: '+fUri);
          });
        },
        // End explicit transaction
        function()
        {
          QUnit.notStrictEqual(player.id, undefined, 'player.id: '+player.id);

          QUnit.start();
        })
      });
      // End atomic operation
    });
  });
});


/**
 * Transaction at KurentoClient
 */
QUnit.asyncTest('Transactional API', function()
{
  var self = this;

  QUnit.expect(2);

  var player;
  var httpGet;

  this.kurento.transaction(function()
  {
    var pipeline = self.pipeline;

    player = pipeline.create('PlayerEndpoint', {uri: URL_SMALL});

    httpGet = this.create('HttpGetEndpoint', {mediaPipeline: pipeline});

    player.connect(httpGet);
  },
  function(error)
  {
    QUnit.equal(error, undefined, 'transaction');

    if(error) return onerror(error);

    httpGet.getUrl(function(error, url)
    {
      if(error) return onerror(error);

      player.release();

      QUnit.notEqual(url, undefined, 'URL: '+url);

      QUnit.start();
    });
  });
});
