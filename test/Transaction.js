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

if (typeof QUnit == 'undefined') {
  QUnit = require('qunit-cli');
  QUnit.load();

  kurentoClient = require('..');

  require('./_common');
  require('./_proxy');
};

var TransactionNotExecutedException = kurentoClient.TransactionsManager.TransactionNotExecutedException;
var TransactionNotCommitedException = kurentoClient.TransactionsManager.TransactionNotCommitedException;
var TransactionRollbackException = kurentoClient.TransactionsManager.TransactionRollbackException;

// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/contains
if (!String.prototype.contains) {
  String.prototype.contains = function () {
    return String.prototype.indexOf.apply(this, arguments) !== -1;
  };
}

QUnit.module('Transaction', lifecycle);

QUnit.asyncTest('transaction', function () {
  QUnit.expect(1);

  // Pipeline creation (no transaction)
  var pipeline = this.pipeline;

  var player = pipeline.create('PlayerEndpoint', {
      uri: URL_SMALL
    },
    function (error) {
      if (error) return onerror(error);

      var httpGet = pipeline.create('HttpGetEndpoint',
        function (error) {
          if (error) return onerror(error);

          player.connect(httpGet, function (error) {
            if (error) return onerror(error);
            // End pipeline creation

            httpGet.getUrl(function (error, url) {
              if (error) return onerror(error);

              // Explicit transaction
              var tx = pipeline.beginTransaction();

              player.play(tx);
              var url_inTransaction = httpGet.getUrl(tx);

              tx.commit(function (error) {
                if (error) return onerror(error);

                QUnit.equal(url_inTransaction.value, url);

                QUnit.start();
              });
              // End explicit transaction
            });
          });
        });
    });
});

QUnit.asyncTest('multiple transaction', function () {
  var self = this;

  QUnit.expect(1);

  var tx1, tx2;
  var promiseUrl1, promiseUrl2;

  // Pipeline creation (transaction)
  tx1 = this.kurento.beginTransaction();
  var pipeline = this.kurento.create(tx1, 'MediaPipeline');

  var httpGet = pipeline.create(tx1, 'HttpGetEndpoint');
  promiseUrl1 = httpGet.getUrl(tx1);
  tx1.commit();
  // End pipeline creation

  // Pipeline creation (transaction)
  tx1.then(function () {
      tx2 = self.kurento.beginTransaction();
      var pipeline = self.kurento.create(tx2, 'MediaPipeline');

      var httpGet = pipeline.create(tx2, 'HttpGetEndpoint');
      promiseUrl2 = httpGet.getUrl(tx2);
      tx2.commit();

      tx2.then(function () {
          QUnit.notEqual(promiseUrl1.value, promiseUrl2.value);

          QUnit.start();
        })
        .catch(onerror)
    })
    .catch(onerror)
    // End pipeline creation
});

QUnit.asyncTest('creation in transaction', function () {
  var self = this;

  QUnit.expect(1);

  var tx1, tx2;
  var promiseUrl1, promiseUrl2;

  // Pipeline creation (transaction)
  tx1 = this.kurento.beginTransaction();
  var pipeline = this.kurento.create(tx1, 'MediaPipeline');

  var player = pipeline.create(tx1, 'PlayerEndpoint', {
    uri: URL_SMALL
  });
  var httpGet = pipeline.create(tx1, 'HttpGetEndpoint');

  player.connect(tx1, httpGet);

  promiseUrl1 = httpGet.getUrl(tx1);
  tx1.commit();
  // End pipeline creation

  // Explicit transaction
  tx1.then(function () {
        tx2 = self.kurento.beginTransaction();
        player.play(tx2);

        promiseUrl2 = httpGet.getUrl(tx2);

        pipeline.release(tx2);
        tx2.commit();

        tx2.then(function () {
            QUnit.equal(promiseUrl1.value, promiseUrl2.value);

            QUnit.start();
          },
          onerror);
      },
      onerror)
    // End explicit transaction
});

QUnit.asyncTest('use plain methods in new objects inside transaction',
  function () {
    QUnit.expect(1);

    var pipeline = this.pipeline;

    // Pipeline creation (no transaction)
    pipeline.create('PlayerEndpoint', {
        uri: URL_SMALL
      })
      .then(function (player) {
        // Creation in explicit transaction
        var tx = pipeline.beginTransaction();
        var httpGet = pipeline.create(tx, 'HttpGetEndpoint');

        httpGet.connect(player, function (error) {
          QUnit.ok(error instanceof TransactionNotCommitedException,
            'error is instance of ' + error.constructor.name);

          QUnit.start();
        });
      });
  });

QUnit.asyncTest(
  'use plain methods with new objects as params inside transaction',
  function () {
    QUnit.expect(1);

    var pipeline = this.pipeline;

    // Pipeline creation (no transaction)
    pipeline.create('PlayerEndpoint', {
        uri: URL_SMALL
      })
      .then(function (player) {
        // Creation in explicit transaction
        var tx = pipeline.beginTransaction();
        var httpGet = pipeline.create(tx, 'HttpGetEndpoint');

        player.connect(httpGet, function (error) {
          QUnit.ok(error instanceof TransactionNotCommitedException,
            'error is instance of ' + error.constructor.name);

          QUnit.start();
        });
      });
  });

QUnit.asyncTest('is commited', function () {
  QUnit.expect(4);

  var tx = this.pipeline.beginTransaction();

  var pipeline = this.kurento.create(tx, 'MediaPipeline');
  var player = this.pipeline.create(tx, 'PlayerEndpoint', {
    uri: URL_SMALL
  });
  var httpGet = this.pipeline.create(tx, 'HttpGetEndpoint');

  player.connect(tx, httpGet);

  QUnit.equal(player.commited, false);
  QUnit.equal(tx.commited, false);

  tx.commit(function () {
    QUnit.strictEqual(player.commited, true);
    QUnit.strictEqual(tx.commited, true);

    QUnit.start();
  });
});

QUnit.asyncTest('user rollback', function () {
  QUnit.expect(2);

  var tx = this.kurento.beginTransaction();

  var pipeline = this.kurento.create(tx, 'MediaPipeline');
  var player = pipeline.create(tx, 'PlayerEndpoint', {
    uri: URL_SMALL
  });
  var promiseUri = player.getUri(tx);

  tx.rollback();

  player.release(function (error) {
    QUnit.ok(error instanceof TransactionRollbackException,
      'error is instance of ' + error.constructor.name);

    QUnit.start();
  });

  QUnit.throws(function () {
      var uri = promiseUri.value;
    },
    TransactionRollbackException);
});

QUnit.asyncTest('transaction error', function () {
  QUnit.expect(11);

  var tx = this.kurento.beginTransaction();

  var pipeline = this.kurento.create(tx, 'MediaPipeline');
  var player = pipeline.create(tx, 'PlayerEndpoint', {
    uri: URL_SMALL
  });

  tx.commit(function (error) {
    QUnit.equal(error, undefined);

    player.release(function (error) {
      QUnit.equal(error, undefined);

      player.play(function (error) {
        QUnit.notEqual(error, undefined);
        QUnit.equal(error.code, 40101);
        QUnit.ok(error.message.contains(" not found"));

        tx = pipeline.beginTransaction();

        var filter = pipeline.create(tx, 'ZBarFilter');
        player.play(tx);

        tx.commit(function (error) {
          QUnit.notEqual(error, undefined);
          QUnit.equal(error.code, 40101);
          QUnit.ok(error.message.contains(" not found"));

          filter.connect(player, function (error) {
            QUnit.notEqual(error, undefined);
            QUnit.equal(error.code, 40101);
            QUnit.ok(error.message.contains(
              " not found"));

            QUnit.start();
          });
        });
      })
    });
  });
});

QUnit.asyncTest('Transaction object on pseudo-sync API', function () {
  var self = this;

  QUnit.expect(5);

  var pipeline = self.pipeline;

  var t = pipeline.beginTransaction();

  var player = pipeline.create(t, 'PlayerEndpoint', {
    uri: URL_SMALL
  });
  var httpGet = pipeline.create(t, 'HttpGetEndpoint');

  player.connect(t, httpGet);

  var promiseUrl = httpGet.getUrl(t);

  player.play(t);

  QUnit.strictEqual(player.id, undefined);

  t.commit(function (error) {
    QUnit.equal(error, undefined, 'commit');

    QUnit.notStrictEqual(player.id, undefined, 'player.id: ' + player.id);

    QUnit.notStrictEqual(promiseUrl.value, undefined, 'httpGet.url: ' +
      promiseUrl.value);

    pipeline.release(function (error) {
      QUnit.equal(error, undefined, 'release');

      if (error) return onerror(error);

      QUnit.start();
    });
  });
});

QUnit.asyncTest('Transaction object on async API', function () {
  var self = this;

  QUnit.expect(4);

  var pipeline = self.pipeline;

  pipeline.create('PlayerEndpoint', {
    uri: URL_SMALL
  }, function (error, player) {
    if (error) return onerror(error);

    pipeline.create('HttpGetEndpoint', function (error, httpGet) {
      if (error) return onerror(error);

      player.connect(httpGet, function (error) {
        QUnit.equal(error, undefined, 'connect');

        if (error) return onerror(error);

        httpGet.getUrl(function (error, url) {
          if (error) return onerror(error);

          var t = pipeline.beginTransaction();

          player.play(t);

          var promiseUrl = httpGet.getUrl(t);

          t.commit(function (error) {
            QUnit.equal(error, undefined, 'commit');

            if (error) return onerror(error);

            promiseUrl.then(function (value) {
              QUnit.equal(value, url, 'URL: ' +
                value);
            })

            pipeline.release(function (error) {
              QUnit.equal(error, undefined,
                'release');

              if (error) return onerror(error);

              QUnit.start();
            });
          });
        });
      });
    });
  });
});

QUnit.asyncTest('transaction creation', function () {
  var self = this;

  QUnit.expect(2);

  // Pipeline creation
  var pipeline = self.pipeline;

  // Atomic creation
  var player = pipeline.create('PlayerEndpoint', {
      uri: URL_SMALL
    },
    function (error) {
      if (error) return onerror(error);
      // End atomic creation

      // Creation in transaction
      var httpGet;
      pipeline.transaction(function () {
          httpGet = pipeline.create('HttpGetEndpoint');

          player.connect(httpGet);
        },
        // End transaction
        function (error) {
          QUnit.equal(error, undefined, 'transaction');

          if (error) return onerror(error);

          httpGet.getUrl(function (error, url) {
            if (error) return onerror(error);

            QUnit.notEqual(url, undefined, 'URL: ' + url);

            QUnit.start();
          });
        });
    });
});

QUnit.asyncTest('use thenable on element', function () {
  var self = this;

  QUnit.expect(1);

  // Pipeline creation (implicit transaction)
  var pipeline = self.pipeline;

  var player = pipeline.create('PlayerEndpoint', {
    uri: URL_SMALL
  });
  var httpGet = pipeline.create('HttpGetEndpoint');

  var connectOp = player.connect(httpGet);

  httpGet.then(function () {
    this.getUrl(function (error, url) {
      if (error) return onerror(error);

      QUnit.notEqual(url, undefined, 'URL: ' + url);

      QUnit.start();
    });
  })
});

QUnit.asyncTest('Use thenable on transaction', function () {
  var self = this;

  QUnit.expect(0);

  // Pipeline creation
  var pipeline = self.pipeline;

  // Atomic creation
  var player = pipeline.create('PlayerEndpoint', {
    uri: URL_SMALL
  });
  // End atomic creation

  // Creation in transaction
  var t = pipeline.transaction(function () {
    var httpGet = pipeline.create('HttpGetEndpoint');

    player.connect(httpGet);
  });
  // End transaction

  t.then(function () {
    player.release();

    QUnit.start();
  });
});

QUnit.asyncTest('Id is set after thenable is resolved', function () {
  var self = this;

  QUnit.expect(2);

  var pipeline = self.pipeline;

  var player = pipeline.create('PlayerEndpoint', {
    uri: URL_SMALL
  });
  var httpGet = pipeline.create('HttpGetEndpoint');

  player.connect(httpGet);

  QUnit.strictEqual(player.id, undefined);

  player.then(function () {
    QUnit.notStrictEqual(player.id, undefined, 'player.id: ' +
      player.id);

    QUnit.start();
  });
});

QUnit.asyncTest('Promise', function () {
  var self = this;

  QUnit.expect(4);

  // Pipeline creation (implicit transaction)
  var pipeline = self.pipeline;

  var player = pipeline.create('PlayerEndpoint', {
    uri: URL_SMALL
  });
  var httpGet = pipeline.create('HttpGetEndpoint');

  player.connect(httpGet);

  // Atomic operation
  httpGet.getUrl(function (error, url) {
    if (error) return onerror(error);

    httpGet.getMediaPipeline(function (error, rPipeline) {
      if (error) return onerror(error);

      player.getUri(function (error, uri) {
        if (error) return onerror(error);

        QUnit.equal(rPipeline, pipeline);

        // Explicit transaction
        pipeline.transaction(function () {
            httpGet.getUrl(function (error, fUrl) {
              QUnit.equal(fUrl, url, 'URL: ' + fUrl);
            });
            player.getUri(function (error, fUri) {
              QUnit.equal(fUri, uri, 'URI: ' + fUri);
            });
          },
          // End explicit transaction
          function () {
            QUnit.notStrictEqual(player.id, undefined,
              'player.id: ' + player.id);

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
QUnit.asyncTest('Transactional API', function () {
  var self = this;

  QUnit.expect(2);

  var player;
  var httpGet;

  this.kurento.transaction(function () {
      var pipeline = self.pipeline;

      player = pipeline.create('PlayerEndpoint', {
        uri: URL_SMALL
      });

      httpGet = this.create('HttpGetEndpoint', {
        mediaPipeline: pipeline
      });

      player.connect(httpGet);
    },
    function (error) {
      QUnit.equal(error, undefined, 'transaction');

      if (error) return onerror(error);

      httpGet.getUrl(function (error, url) {
        if (error) return onerror(error);

        player.release();

        QUnit.notEqual(url, undefined, 'URL: ' + url);

        QUnit.start();
      });
    });
});
