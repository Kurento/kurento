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

QUnit.module(QUnit.config.prefix + 'PlayerEndpoint', lifecycle);

QUnit.asyncTest('Play, Pause & Stop with Callback', function () {
  var self = this;

  QUnit.expect(4);

  self.pipeline.create('PlayerEndpoint', {
      uri: URL_SMALL
    },
    function (error, player) {
      if (error) return onerror(error);

      QUnit.notEqual(player, undefined, 'player');

      return player.play(function (error) {
        QUnit.equal(error, undefined, 'playing');

        if (error) return onerror(error);

        return player.pause(function (error) {
          QUnit.equal(error, undefined, 'paused');

          if (error) return onerror(error);

          return player.stop(function (error) {
            QUnit.equal(error, undefined, 'stoped');

            if (error) return onerror(error);

            QUnit.start();
          });
        });
      });
    })
    .catch(onerror)
});

QUnit.asyncTest('Play, Pause & Stop with Promise', function () {
  var self = this;

  QUnit.expect(1);

  self.pipeline.create('PlayerEndpoint', {
      uri: URL_SMALL
    }).then(function (player) {
      QUnit.notEqual(player, undefined, 'player');

      return player.play().then(function () {
        return player.pause().then(function () {
          return player.stop().then(function () {
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
    }, function(error) {
        if (error) return onerror(error)
      })
    .catch(onerror)
});

QUnit.asyncTest('End of Stream with Callback', function (assert) {
  var self = this;

  assert.expect(2);

  var timeout = new Timeout('"PlayerEndpoint:End of Stream"',
    15 * 1000, onerror);

  function onerror(error) {
    timeout.stop();
    _onerror(error);
  };

  self.pipeline.create('PlayerEndpoint', {
    uri: URL_SMALL
  }, function (error, player) {
    if (error) return onerror(error);

    player.on('EndOfStream', function (data) {
      assert.ok(true, 'EndOfStream');

      timeout.stop();

      QUnit.start();
    })
    .catch(onerror)

    return player.play(function (error) {
      assert.equal(error, undefined, 'playing');

      if (error) return onerror(error);

      timeout.start();
    });
  })
  .catch(onerror)
});

QUnit.asyncTest('End of Stream with Promise', function (assert) {
  var self = this;

  assert.expect(2);

  var timeout = new Timeout('"PlayerEndpoint:End of Stream"',
    15 * 1000, onerror);

  function onerror(error) {
    timeout.stop();
    _onerror(error);
  };

  self.pipeline.create('PlayerEndpoint', {
    uri: URL_SMALL
  }).then(function (player) {
   QUnit.notEqual(player, undefined, 'player');

    player.on('EndOfStream', function (data) {
      assert.ok(true, 'EndOfStream');

      timeout.stop();

      QUnit.start();
    })

    return player.play().then(function () {
      timeout.start();
    }, function(error) {
        if (error) return onerror(error)
      });
  }, function(error) {
      if (error) return onerror(error)
    })
  .catch(onerror)
});

QUnit.asyncTest('GetUri with Callback', function () {
  var self = this;

  QUnit.expect(1);

  self.pipeline.create('PlayerEndpoint', {
    uri: URL_SMALL
  }, function (error, player) {
    if (error) return onerror(error);

    return player.getUri(function (error, url) {
      if (error) return onerror(error);

      QUnit.equal(url, URL_SMALL, 'URL: ' + url);

      QUnit.start();
    });
  })
  .catch(onerror)
});

QUnit.asyncTest('GetUri with Promise', function () {
  var self = this;

  QUnit.expect(2);

  self.pipeline.create('PlayerEndpoint', {
    uri: URL_SMALL
  }).then(function (player) {
      QUnit.notEqual(player, undefined, 'player');

    return player.getUri().then(function (url) {
      QUnit.equal(url, URL_SMALL, 'URL: ' + url);

      QUnit.start();
    }, function(error) {
      if (error) return onerror(error)
    });
  }, function(error) {
      if (error) return onerror(error)
    })
  .catch(onerror)
});

QUnit.asyncTest('Connect with Callback', function () {
  var self = this;

  QUnit.expect(4);

  self.pipeline.create('PlayerEndpoint', {
    uri: URL_SMALL
  }, function (error, player) {
    if (error) return onerror(error);

    return self.pipeline.create('RecorderEndpoint', {
      uri: URL_SMALL
    }, function (error, recorder) {
      if (error) return onerror(error);

      return player.connect(recorder, function (error) {
        QUnit.equal(error, undefined, 'connect');

        if (error) return onerror(error);

        return player.play(function (error) {
          QUnit.equal(error, undefined, 'playing');

          if (error) return onerror(error);

          return recorder.release(function (error) {
            QUnit.equal(error, undefined,
              'release recorder');

            if (error) return onerror(error);

            return player.release(function (error) {
              QUnit.equal(error, undefined,
                'release player');

              if (error) return onerror(error);

              QUnit.start();
            });
          });
        });
      });
    });
  })
  .catch(onerror)
});

QUnit.asyncTest('Connect with Promise', function () {
  var self = this;

  QUnit.expect(2);

  self.pipeline.create('PlayerEndpoint', {
    uri: URL_SMALL
  }).then(function (player) {
    QUnit.notEqual(player, undefined, 'player');

    return self.pipeline.create('RecorderEndpoint', {
      uri: URL_SMALL
    }).then(function (recorder) {
      QUnit.notEqual(recorder, undefined, 'recorder');

      return player.connect(recorder).then(function () {
        return player.play().then(function () {
          return recorder.release().then(function () {
            return player.release().then(function () {
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
      }, function(error) {
          if (error) return onerror(error)
        });
    }, function(error) {
        if (error) return onerror(error)
      });
  }, function(error) {
      if (error) return onerror(error)
    })
  .catch(onerror)
});

QUnit.asyncTest('Connect by type with Callback', function () {
  var self = this;

  QUnit.expect(5);

  self.pipeline.create('PlayerEndpoint', {
    uri: URL_SMALL
  }, function (error, player) {
    if (error) return onerror(error);

    return self.pipeline.create('RecorderEndpoint', {
      uri: URL_SMALL
    }, function (error, recorder) {
      if (error) return onerror(error);

      return player.connect(recorder, 'AUDIO', function (error) {
        QUnit.equal(error, undefined, 'connect AUDIO');

        if (error) return onerror(error);

        return player.connect(recorder, 'VIDEO', function (error) {
          QUnit.equal(error, undefined, 'connect VIDEO');

          if (error) return onerror(error);

          return player.play(function (error) {
            QUnit.equal(error, undefined, 'play');

            if (error) return onerror(error);

            return recorder.release(function (error) {
              QUnit.equal(error, undefined, 'release recorder');

              if (error) return onerror(error);

              return player.release(function (error) {
                QUnit.equal(error, undefined, 'release player');

                if (error) return onerror(error);

                QUnit.start();
              });
            });
          });
        });
      });
    });
  })
  .catch(onerror)
});

QUnit.asyncTest('Connect by type with Promise', function () {
  var self = this;

  QUnit.expect(2);

  self.pipeline.create('PlayerEndpoint', {
    uri: URL_SMALL
  }).then(function (player) {
    QUnit.notEqual(player, undefined, 'player');

    return self.pipeline.create('RecorderEndpoint', {
      uri: URL_SMALL
    }).then(function (recorder) {
      QUnit.notEqual(recorder, undefined, 'recorder');

      return player.connect(recorder, 'AUDIO').then(function () {
        return player.connect(recorder, 'VIDEO').then(function () {
          return player.play().then(function () {
            return recorder.release().then(function () {
              return player.release().then(function () {
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
        }, function(error) {
            if (error) return onerror(error)
          });
      }, function(error) {
          if (error) return onerror(error)
        });
    }, function(error) {
        if (error) return onerror(error)
      });
  }, function(error) {
      if (error) return onerror(error)
    })
  .catch(onerror)
});
