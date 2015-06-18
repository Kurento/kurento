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
  QUnit.expect(0);

  // Pipeline creation (no transaction)
  var pipeline = this.pipeline;

  var player = pipeline.create('PlayerEndpoint', {
      uri: URL_SMALL
    },
    function (error) {
      if (error) return onerror(error);

      var recorder = pipeline.create('RecorderEndpoint', {
          uri: URL_SMALL
        },
        function (error) {
          if (error) return onerror(error);

          player.connect(recorder, function (error) {
            if (error) return onerror(error);
            // End pipeline creation

            // Explicit transaction
            var tx = pipeline.beginTransaction();

            player.play(tx);
            tx.commit(function (error) {
              if (error) return onerror(error);

              QUnit.start();
            });
            // End explicit transaction
          });
        });
    });
});

QUnit.asyncTest('multiple transaction', function () {
  var self = this;

  QUnit.expect(0);

  var tx1, tx2;

  // Pipeline creation (transaction)
  tx1 = this.kurento.beginTransaction();
  var pipeline = this.kurento.create(tx1, 'MediaPipeline');

  pipeline.create(tx1, 'RecorderEndpoint', {
    uri: URL_SMALL
  });
  tx1.commit();
  // End pipeline creation

  // Pipeline creation (transaction)
  tx1.then(function () {
      tx2 = self.kurento.beginTransaction();
      var pipeline = self.kurento.create(tx2, 'MediaPipeline');

      pipeline.create(tx2, 'RecorderEndpoint', {
        uri: URL_SMALL
      });
      tx2.commit();

      tx2.then(function () {
          QUnit.start();
        })
        .catch(onerror)
    })
    .catch(onerror)
    // End pipeline creation
});

QUnit.asyncTest('creation in transaction', function () {
  var self = this;

  QUnit.expect(0);

  var tx1, tx2;

  // Pipeline creation (transaction)
  tx1 = this.kurento.beginTransaction();
  var pipeline = this.kurento.create(tx1, 'MediaPipeline');

  var player = pipeline.create(tx1, 'PlayerEndpoint', {
    uri: URL_SMALL
  });
  var recorder = pipeline.create(tx1, 'RecorderEndpoint', {
    uri: URL_SMALL
  });

  player.connect(tx1, recorder);

  tx1.commit();
  // End pipeline creation

  // Explicit transaction
  tx1.then(function () {
        tx2 = self.kurento.beginTransaction();
        player.play(tx2);

        pipeline.release(tx2);
        tx2.commit();

        tx2.then(function () {
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
        var recorder = pipeline.create(tx, 'RecorderEndpoint', {
          uri: URL_SMALL
        });

        recorder.connect(player, function (error) {
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
        var recorder = pipeline.create(tx, 'RecorderEndpoint', {
          uri: URL_SMALL
        });

        player.connect(recorder, function (error) {
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
  var recorder = this.pipeline.create(tx, 'RecorderEndpoint', {
    uri: URL_SMALL
  });

  player.connect(tx, recorder);

  QUnit.equal(player.commited, false);
  QUnit.equal(tx.commited, false);

  tx.commit(function () {
    QUnit.strictEqual(player.commited, true);
    QUnit.strictEqual(tx.commited, true);

    QUnit.start();
  });
});

QUnit.asyncTest('user rollback', function () {
  QUnit.expect(1);

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

  QUnit.expect(4);

  var pipeline = self.pipeline;

  var t = pipeline.beginTransaction();

  var player = pipeline.create(t, 'PlayerEndpoint', {
    uri: URL_SMALL
  });
  var recorder = pipeline.create(t, 'RecorderEndpoint', {
    uri: URL_SMALL
  });

  player.connect(t, recorder);

  player.play(t);

  QUnit.strictEqual(player.id, undefined);

  t.commit(function (error) {
    QUnit.equal(error, undefined, 'commit');

    QUnit.notStrictEqual(player.id, undefined, 'player.id: ' + player.id);

    pipeline.release(function (error) {
      QUnit.equal(error, undefined, 'release');

      if (error) return onerror(error);

      QUnit.start();
    });
  });
});

QUnit.asyncTest('Transaction object on async API', function (assert) {
  var self = this;

  assert.expect(3);

  var pipeline = self.pipeline;

  pipeline.create('PlayerEndpoint', {
    uri: URL_SMALL
  }, function (error, player) {
    if (error) return onerror(error);

    pipeline.create('RecorderEndpoint', {
      uri: URL_SMALL
    }, function (error, recorder) {
      if (error) return onerror(error);

      player.connect(recorder, function (error) {
        assert.equal(error, undefined, 'connect');

        if (error) return onerror(error);

        var t = pipeline.beginTransaction();

        player.play(t);

        t.commit(function (error) {
          assert.equal(error, undefined, 'commit');

          if (error) return onerror(error);

          pipeline.release(function (error) {
            assert.equal(error, undefined,
              'release');

            if (error) return onerror(error);

            QUnit.start();
          });
        });
      });
    });
  });
});

QUnit.asyncTest('transaction creation', function (assert) {
  var self = this;

  assert.expect(1);

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
      pipeline.transaction(function () {
          var recorder = pipeline.create('RecorderEndpoint', {
            uri: URL_SMALL
          });

          player.connect(recorder);
        },
        // End transaction
        function (error) {
          assert.equal(error, undefined, 'transaction');

          if (error) return onerror(error);

          QUnit.start();
        });
    });
});

QUnit.asyncTest('use thenable on element', function (assert) {
  var self = this;

  assert.expect(0);

  // Pipeline creation (implicit transaction)
  var pipeline = self.pipeline;

  var player = pipeline.create('PlayerEndpoint', {
    uri: URL_SMALL
  });
  var recorder = pipeline.create('RecorderEndpoint', {
    uri: URL_SMALL
  });

  player.connect(recorder);

  recorder.then(function () {
    QUnit.start();
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
    var recorder = pipeline.create('RecorderEndpoint', {
      uri: URL_SMALL
    });

    player.connect(recorder);
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
  var recorder = pipeline.create('RecorderEndpoint', {
    uri: URL_SMALL
  });

  player.connect(recorder);

  QUnit.strictEqual(player.id, undefined);

  player.then(function () {
    QUnit.notStrictEqual(player.id, undefined, 'player.id: ' +
      player.id);

    QUnit.start();
  });
});

QUnit.asyncTest('Promise', function () {
  var self = this;

  QUnit.expect(3);

  // Pipeline creation (implicit transaction)
  var pipeline = self.pipeline;

  var player = pipeline.create('PlayerEndpoint', {
    uri: URL_SMALL
  });
  var recorder = pipeline.create('RecorderEndpoint', {
    uri: URL_SMALL
  });

  player.connect(recorder);

  // Atomic operation
  recorder.getMediaPipeline(function (error, rPipeline) {
    if (error) return onerror(error);

    player.getUri(function (error, uri) {
      if (error) return onerror(error);

      QUnit.equal(rPipeline, pipeline);

      // Explicit transaction
      pipeline.transaction(function () {
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
  });
  // End atomic operation
});

/**
 * Transaction at KurentoClient
 */
QUnit.asyncTest('Transactional API', function (assert) {
  var self = this;

  assert.expect(1);

  var player;

  this.kurento.transaction(function () {
      var pipeline = self.pipeline;

      player = pipeline.create('PlayerEndpoint', {
        uri: URL_SMALL
      });

      var recorder = this.create('RecorderEndpoint', {
        mediaPipeline: pipeline,
        uri: URL_SMALL
      });

      player.connect(recorder);
    },
    function (error) {
      assert.equal(error, undefined, 'transaction');

      if (error) return onerror(error);

      player.release();

      QUnit.start();
    });
});

/**
 * Transaction at KurentoClient
 */
QUnit.asyncTest('Auto-transactions', function (assert) {
  var self = this;

  assert.expect(1);

  var player;
  var recorder;

  var pipeline = self.pipeline;

  recorder = this.kurento.create('RecorderEndpoint', {
    mediaPipeline: pipeline,
    uri: URL_SMALL
  });

  this.kurento.transaction(function () {
      player = pipeline.create('PlayerEndpoint', {
        uri: URL_SMALL
      });

      player.connect(recorder);
    },
    function (error) {
      assert.equal(error, undefined, 'transaction');

      if (error) return onerror(error);

      QUnit.start();
    });

  player.release();
});
