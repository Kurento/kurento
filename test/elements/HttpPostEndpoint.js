/*
 * (C) Copyright 2013-2014 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

if (typeof QUnit == 'undefined') {
  QUnit = require('qunit-cli');
  QUnit.load();

  kurentoClient = require('..');

  require('./_common');
  require('./_proxy');
};

if (QUnit.config.prefix == undefined)
  QUnit.config.prefix = '';

QUnit.module(QUnit.config.prefix + 'HttpPostEndpoint', lifecycle);

QUnit.asyncTest('Method GetUrl with Callback', function () {
  var self = this;

  QUnit.expect(2);

  self.pipeline.create('HttpPostEndpoint', function (error, httpPost) {
    if (error) return onerror(error);

    QUnit.notEqual(httpPost, undefined, 'httpPost');

    return httpPost.getUrl(function (error, url) {
      if (error) return onerror(error);

      QUnit.notEqual(url, undefined, 'URL: ' + url);

      QUnit.start();
    })
  })
  .catch(onerror)
});

QUnit.asyncTest('Method GetUrl with Promise', function () {
  var self = this;

  QUnit.expect(2);

  self.pipeline.create('HttpPostEndpoint').then(function (httpPost) {
    QUnit.notEqual(httpPost, undefined, 'httpPost');

    return httpPost.getUrl().then(function (url) {
      QUnit.notEqual(url, undefined, 'URL: ' + url);

      QUnit.start();
    }, function(error) {
        if (error) return onerror(error)
      })
  }, function(error) {
      if (error) return onerror(error)
    })
  .catch(onerror)
});

//QUnit.asyncTest('Media session terminated', function()
//{
//  var self = this;

//  QUnit.expect(3);

//  var timeout = new Timeout('"HttpPostEndpoint:Media session terminated"',
//                            50 * 1000, onerror);

//  function onerror(error)
//  {
//    timeout.stop();
//    _onerror(error);
//  };

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
//      doGet(url, timeout.start.bind(timeout), onerror);
//    })
//  });
//});
