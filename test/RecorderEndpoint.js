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


QUnit.module('RecorderEndpoint', lifecycle);

QUnit.asyncTest('Record, Pause & Stop', function()
{
  var self = this;

  QUnit.expect(4);

  self.pipeline.create('RecorderEndpoint', {uri: URL_SMALL},
  function(error, recorder)
  {
    if(error) return onerror(error);

    QUnit.notEqual(recorder, undefined, 'recorder');

    recorder.record(function(error)
    {
      if(error) return onerror(error);

      QUnit.ok(true, 'record');

      recorder.pause(function(error)
      {
        if(error) return onerror(error);

        QUnit.ok(true, 'pause');

        recorder.stop(function(error)
        {
          if(error) return onerror(error);

          QUnit.ok(true, 'stop');

          QUnit.start();
        });
      });
    });
  });
});

QUnit.asyncTest('GetUrl', function()
{
  var self = this;

  QUnit.expect(1);

  self.pipeline.create('RecorderEndpoint', {uri: URL_SMALL},
  function(error, recorder)
  {
    if(error) return onerror(error);

    recorder.getUri(function(error, uri)
    {
      if(error) return onerror(error);

      QUnit.equal(uri, URL_SMALL, 'URI: '+uri);

      QUnit.start();
    });
  });
});
