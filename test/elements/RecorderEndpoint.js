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

QUnit.module(QUnit.config.prefix + 'RecorderEndpoint', lifecycle);

QUnit.asyncTest('Record, Pause & Stop with Callback', function () {
  var self = this;

  QUnit.expect(4);

  self.pipeline.create(QUnit.config.prefix + 'RecorderEndpoint', {
      uri: URL_SMALL
    },
    function (error, recorder) {
      if (error) return onerror(error);

      QUnit.notEqual(recorder, undefined, 'recorder');

      return recorder.record(function (error) {
        QUnit.equal(error, undefined, 'record');

        if (error) return onerror(error);

        return recorder.pause(function (error) {
          QUnit.equal(error, undefined, 'pause');

          if (error) return onerror(error);

          return recorder.stop(function (error) {
            QUnit.equal(error, undefined, 'stop');

            if (error) return onerror(error);

            QUnit.start();
          });
        });
      });
    })
    .catch(onerror)
});

QUnit.asyncTest('Record, Pause & Stop with Promise', function () {
  var self = this;

  QUnit.expect(1);

  self.pipeline.create(QUnit.config.prefix + 'RecorderEndpoint', {
      uri: URL_SMALL
    }).then(function (recorder) {

      QUnit.notEqual(recorder, undefined, 'recorder');

      return recorder.record().then(function () {
        return recorder.pause().then(function () {
          return recorder.stop().then(function () {

            QUnit.start();
          }, function(error) {
              if (error) return onerror(error)
            });
        }, function(error) {
            if (error) return onerror(error)
         });
      }, function(error) {
          if (error) return onerror(error)
        });
    })
    .catch(onerror)
});

QUnit.asyncTest('GetUrl with Callback', function () {
  var self = this;

  QUnit.expect(1);

  self.pipeline.create(QUnit.config.prefix + 'RecorderEndpoint', {
      uri: URL_SMALL
    },
    function (error, recorder) {
      if (error) return onerror(error);

      return recorder.getUri(function (error, uri) {
        if (error) return onerror(error);

        QUnit.equal(uri, URL_SMALL, 'URI: ' + uri);

        QUnit.start();
      });
    })
    .catch(onerror)
});

QUnit.asyncTest('GetUrl with Promise', function () {
  var self = this;

  QUnit.expect(2);

  self.pipeline.create(QUnit.config.prefix + 'RecorderEndpoint', {
      uri: URL_SMALL
    }).then(function (recorder) {
      QUnit.notEqual(recorder, undefined, 'recorder');

      return recorder.getUri().then(function (uri) {
        QUnit.equal(uri, URL_SMALL, 'URI: ' + uri);

        QUnit.start();
      }), function(error) {
          if (error) return onerror(error)
        };
    }, function(error) {
          if (error) return onerror(error)
        })
    .catch(onerror)
});
