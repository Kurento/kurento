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
 * {@link MediaPipeline} creation test suite.
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


QUnit.module('MediaPipelineCreation', lifecycle);

QUnit.asyncTest('normal use', function()
{
  var self = this;

  QUnit.expect(3);

  var pipeline = self.pipeline;

  var player  = pipeline.create('PlayerEndpoint', {uri: URL_SMALL});
  var httpGet = pipeline.create('HttpGetEndpoint');

  player.connect(httpGetEndpoint);

  pipeline.start(function(error)
  {
    if(error) return onerror(error);

    httpGet.getUrl(function(url)
    {
      player.release();

      QUnit.notEqual(url, undefined, 'URL: '+url);

      QUnit.start();
    });
  });
});

QUnit.asyncTest('early pipeline creation', function()
{
  var self = this;

  QUnit.expect(3);

  var pipeline = self.pipeline;

  pipeline.start(function(error)
  {
    if(error) return onerror(error);

    var player  = pipeline.create('PlayerEndpoint', {uri: URL_SMALL});
    var httpGet = pipeline.create('HttpGetEndpoint');

    player.connect(httpGetEndpoint);

    httpGet.getUrl(function(url)
    {
      player.release();

      QUnit.notEqual(url, undefined, 'URL: '+url);

      pipeline.release();

      QUnit.start();
    });
  });
});

QUnit.asyncTest('non started', function()
{
  var self = this;

  QUnit.expect(3);

  var pipeline = self.pipeline;

  var player = pipeline.create('PlayerEndpoint', {uri: URL_SMALL});

  QUnit.throws(function()
  {
    player.play();
  });
});


QUnit.asyncTest('started twice', function()
{
  var self = this;

  QUnit.expect(3);

  var pipeline = self.pipeline;

  pipeline.start();

  QUnit.throws(function()
  {
    pipeline.start();
  });
});
