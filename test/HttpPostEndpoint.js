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


QUnit.module('HttpPostEndpoint', lifecycle);

QUnit.asyncTest('Method GetUrl', function()
{
  var self = this;

  QUnit.expect(2);

  self.pipeline.create('HttpPostEndpoint', function(error, httpPost)
  {
    if(error) return onerror(error);

    QUnit.notEqual(httpPost, undefined, 'httpPost');

    httpPost.getUrl(function(error, url)
    {
      if(error) return onerror(error);

      QUnit.notEqual(url, undefined, 'URL: '+url);

      QUnit.start();
    })
  });
});

//QUnit.asyncTest('Media session terminated', function()
//{
//  var self = this;

//  QUnit.expect(3);


//  var timeout = new Timeout('"HttpPostEndpoint:Media session terminated"',
//                            50 * 1000, onerror);


//  self.pipeline.create('HttpPostEndpoint', function(error, httpPost)
//  {
//    if(error) return onerror(error);

//    QUnit.notEqual(httpPost, undefined, 'httpPost');

//    httpPost.on('MediaSessionTerminated', function(data)
//    {
//      QUnit.ok(true, 'MediaSessionTerminated');

//      timeout.stop();

//      QUnit.start();
//    });

//    httpPost.getUrl(function(error, url)
//    {
//      if(error) return onerror(error);

//      QUnit.notEqual(url, undefined, 'URL: '+url);

//      // This should trigger MediaSessionTerminated event
//      doGet(url,
//      function()
//      {
//        timeout.start()
//      },
//      onerror);
//    })
//  });
//});
