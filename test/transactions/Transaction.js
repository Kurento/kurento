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

if (QUnit.config.prefix == undefined)
  QUnit.config.prefix = '';

var TransactionNotExecutedException = kurentoClient.TransactionsManager.TransactionNotExecutedException;
var TransactionNotCommitedException = kurentoClient.TransactionsManager.TransactionNotCommitedException;
var TransactionRollbackException = kurentoClient.TransactionsManager.TransactionRollbackException;

// https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/String/contains
if (!String.prototype.contains) {
  String.prototype.contains = function () {
    return String.prototype.indexOf.apply(this, arguments) !== -1;
  };
}

QUnit.module(QUnit.config.prefix + 'Transaction', lifecycle);

/**
 * Transaction at KurentoClient
 */
QUnit.asyncTest('Auto-transactions', function (assert) {
  var self = this;

  assert.expect(1);

  var player;
  var recorder;

  var pipeline = self.pipeline;

  recorder = this.kurento.create(QUnit.config.prefix + 'RecorderEndpoint', {
    mediaPipeline: pipeline,
    uri: URL_SMALL
  });

  this.kurento.transaction(function () {
        player = pipeline.create(QUnit.config.prefix + 'PlayerEndpoint', {
          uri: URL_SMALL
        });

        return player.connect(recorder);
      },
      function (error) {
        assert.equal(error, undefined, 'transaction');

        if (error) return onerror(error);

        QUnit.start();
      })
    .catch(onerror)
});

QUnit.asyncTest('transaction error', function (assert) {
  assert.expect(8);

  var tx = this.kurento.beginTransaction();

  var pipeline = this.kurento.create(tx, QUnit.config.prefix + 'MediaPipeline');
  var player = pipeline.create(tx, QUnit.config.prefix + 'PlayerEndpoint', {
    uri: URL_SMALL
  });
  tx.commit(function (error) {
      assert.equal(error, undefined);

      return player.release(function (error) {
        assert.equal(error, undefined);

        return player.play(function (error) {
          assert.notEqual(error, undefined);
          assert.equal(error.code, 40101);
          assert.ok(error.message.contains(" not found"));

          tx = pipeline.beginTransaction();
          var filter = pipeline.create(tx, QUnit.config.prefix + 'ZBarFilter');
          player.play(tx);

          return tx.commit(function (error) {

            return filter.connect(player, function (error) {
              assert.notEqual(error, undefined);
              assert.equal(error.code, 40101);
              assert.ok(error.message.contains(
                " not found"));

              QUnit.start();
            })
          })
        })
      })
    })
    .catch(onerror)
});

QUnit.asyncTest('transaction', function (assert) {
  assert.expect(0);

  // Pipeline creation (no transaction)
  var pipeline = this.pipeline;

  pipeline.create(QUnit.config.prefix + 'PlayerEndpoint', {
        uri: URL_SMALL
      },
      function (error, player) {
        if (error) return onerror(error);

        return pipeline.create(QUnit.config.prefix + 'RecorderEndpoint', {
            uri: URL_SMALL
          },
          function (error, recorder) {
            if (error) return onerror(error);

            return player.connect(recorder, function (error) {
              if (error) return onerror(error);
              // End pipeline creation

              // Explicit transaction
              var tx = pipeline.beginTransaction();

              player.play(tx);

              return tx.commit(function (error) {
                if (error) return onerror(error);

                QUnit.start();
              });
              // End explicit transaction
            });
          });
      })
    .catch(onerror)
});

QUnit.asyncTest('multiple transaction', function (assert) {
  var self = this;

  assert.expect(0);

  var tx1, tx2;

  // Pipeline creation (transaction)
  tx1 = this.kurento.beginTransaction();
  var pipeline = this.kurento.create(tx1, QUnit.config.prefix + 'MediaPipeline');

  pipeline.create(tx1, QUnit.config.prefix + 'RecorderEndpoint', {
    uri: URL_SMALL
  });
  tx1.commit();
  // End pipeline creation

  // Pipeline creation (transaction)
  tx1.then(function () {
      tx2 = self.kurento.beginTransaction();
      var pipeline = self.kurento.create(tx2, QUnit.config.prefix + 'MediaPipeline');

      pipeline.create(tx2, QUnit.config.prefix + 'RecorderEndpoint', {
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

QUnit.asyncTest('creation in transaction', function (assert) {
  var self = this;

  assert.expect(0);

  var tx1, tx2;

  // Pipeline creation (transaction)
  tx1 = this.kurento.beginTransaction();
  var pipeline = this.kurento.create(tx1, QUnit.config.prefix + 'MediaPipeline');

  var player = pipeline.create(tx1, QUnit.config.prefix + 'PlayerEndpoint', {
    uri: URL_SMALL
  });
  var recorder = pipeline.create(tx1, QUnit.config.prefix + 'RecorderEndpoint', {
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

        return tx2.then(QUnit.start.bind(QUnit), onerror);
      },
      onerror)
    .catch(onerror)
    // End explicit transaction
});

QUnit.asyncTest('use plain methods in new objects inside transaction',
  function (assert) {
    assert.expect(1);

    var pipeline = this.pipeline;

    // Pipeline creation (no transaction)
    pipeline.create(QUnit.config.prefix + 'PlayerEndpoint', {
        uri: URL_SMALL
      })
      .then(function (player) {
        // Creation in explicit transaction
        var tx = pipeline.beginTransaction();
        var recorder = pipeline.create(tx, QUnit.config.prefix + 'RecorderEndpoint', {
          uri: URL_SMALL
        });

        return recorder.connect(player, function (error) {
          assert.ok(error instanceof TransactionNotCommitedException,
            'error is instance of ' + error.constructor.name);

          QUnit.start();
        });
      })
      .catch(onerror)
  });

QUnit.asyncTest(
  'use plain methods with new objects as params inside transaction',
  function (assert) {
    assert.expect(1);

    var pipeline = this.pipeline;

    // Pipeline creation (no transaction)
    pipeline.create(QUnit.config.prefix + 'PlayerEndpoint', {
        uri: URL_SMALL
      })
      .then(function (player) {
        // Creation in explicit transaction
        var tx = pipeline.beginTransaction();
        var recorder = pipeline.create(tx, QUnit.config.prefix + 'RecorderEndpoint', {
          uri: URL_SMALL
        });

        return player.connect(recorder, function (error) {
          assert.ok(error instanceof TransactionNotCommitedException,
            'error is instance of ' + error.constructor.name);

          QUnit.start();
        });
      })
      .catch(onerror)
  });

QUnit.asyncTest('is commited', function (assert) {
  assert.expect(4);

  var tx = this.pipeline.beginTransaction();

  var pipeline = this.kurento.create(tx, QUnit.config.prefix + 'MediaPipeline');
  var player = this.pipeline.create(tx, QUnit.config.prefix + 'PlayerEndpoint', {
    uri: URL_SMALL
  });
  var recorder = this.pipeline.create(tx, QUnit.config.prefix + 'RecorderEndpoint', {
    uri: URL_SMALL
  });

  player.connect(tx, recorder)

  assert.equal(player.commited, false);
  assert.equal(tx.commited, false);

  tx.commit(function () {
      assert.strictEqual(player.commited, true);
      assert.strictEqual(tx.commited, true);

      QUnit.start();
    })
    .catch(onerror)
});

QUnit.asyncTest('user rollback', function (assert) {
  QUnit.expect(1);

  var tx = this.kurento.beginTransaction();

  var pipeline = this.kurento.create(tx, QUnit.config.prefix + 'MediaPipeline');
  var player = pipeline.create(tx, QUnit.config.prefix + 'PlayerEndpoint', {
    uri: URL_SMALL
  });
  var promiseUri = player.getUri(tx);

  tx.rollback();

  player.release(function (error) {
      assert.ok(error instanceof TransactionRollbackException,
        'error is instance of ' + error.constructor.name);

      QUnit.start();
    })
    .catch(onerror)
});

QUnit.asyncTest('Transaction object on pseudo-sync API', function (assert) {
  var self = this;

  assert.expect(4);

  var pipeline = self.pipeline;

  var t = pipeline.beginTransaction();

  var player = pipeline.create(t, QUnit.config.prefix + 'PlayerEndpoint', {
    uri: URL_SMALL
  });
  var recorder = pipeline.create(t, QUnit.config.prefix + 'RecorderEndpoint', {
    uri: URL_SMALL
  });

  player.connect(t, recorder);

  player.play(t);

  assert.strictEqual(player.id, undefined);

  t.commit(function (error) {
      assert.equal(error, undefined, 'commit');

      assert.notStrictEqual(player.id, undefined, 'player.id: ' + player.id);

      return pipeline.release(function (error) {
        assert.equal(error, undefined, 'release');

        if (error) return onerror(error);

        QUnit.start();
      })
    })
    .catch(onerror)
});

QUnit.asyncTest('Transaction object on async API', function (assert) {
  var self = this;

  assert.expect(3);

  var pipeline = self.pipeline;

  pipeline.create(QUnit.config.prefix + 'PlayerEndpoint', {
      uri: URL_SMALL
    }, function (error, player) {
      if (error) return onerror(error);

      return pipeline.create(QUnit.config.prefix + 'RecorderEndpoint', {
        uri: URL_SMALL
      }, function (error, recorder) {
        if (error) return onerror(error);

        return player.connect(recorder, function (error) {
          assert.equal(error, undefined, 'connect');

          if (error) return onerror(error);

          var t = pipeline.beginTransaction();

          player.play(t);

          return t.commit(function (error) {
            assert.equal(error, undefined, 'commit');

            if (error) return onerror(error);

            return pipeline.release(function (error) {
              assert.equal(error, undefined, 'release');

              if (error) return onerror(error);
              QUnit.start();
            })
          })
        })
      })
    })
    .catch(onerror)
});

QUnit.asyncTest('transaction creation', function (assert) {
  var self = this;

  assert.expect(1);

  // Pipeline creation
  var pipeline = self.pipeline;

  // Atomic creation
  pipeline.create(QUnit.config.prefix + 'PlayerEndpoint', {
        uri: URL_SMALL
      },
      function (error, player) {
        if (error) return onerror(error);
        // End atomic creation

        // Creation in transaction
        return pipeline.transaction(function () {
            var recorder = pipeline.create(QUnit.config.prefix + 'RecorderEndpoint', {
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
      })
    .catch(onerror)
});

QUnit.asyncTest('use thenable on element', function (assert) {
  var self = this;

  assert.expect(0);

  // Pipeline creation (implicit transaction)
  var pipeline = self.pipeline;

  var player = pipeline.create(QUnit.config.prefix + 'PlayerEndpoint', {
    uri: URL_SMALL
  });
  var recorder = pipeline.create(QUnit.config.prefix + 'RecorderEndpoint', {
    uri: URL_SMALL
  });

  player.connect(recorder)
    .catch(onerror)

  recorder.then(QUnit.start.bind(QUnit))
    .catch(onerror)
});

QUnit.asyncTest('Use thenable on transaction', function (assert) {
  var self = this;

  assert.expect(0);

  // Pipeline creation
  var pipeline = self.pipeline;

  // Atomic creation
  var player = pipeline.create(QUnit.config.prefix + 'PlayerEndpoint', {
    uri: URL_SMALL
  });
  // End atomic creation

  // Creation in transaction
  var t = pipeline.transaction(function () {
      var recorder = pipeline.create(QUnit.config.prefix + 'RecorderEndpoint', {
        uri: URL_SMALL
      });

      player.connect(recorder);
    })
    .catch(onerror)
    // End transaction

  t.then(function () {
      player.release();

      QUnit.start();
    })
    .catch(onerror)
});

QUnit.asyncTest('Id is set after thenable is resolved', function (assert) {
  var self = this;

  assert.expect(2);

  var pipeline = self.pipeline;

  var player = pipeline.create(QUnit.config.prefix + 'PlayerEndpoint', {
    uri: URL_SMALL
  });
  var recorder = pipeline.create(QUnit.config.prefix + 'RecorderEndpoint', {
    uri: URL_SMALL
  });

  player.connect(recorder)
    .catch(onerror)

  assert.strictEqual(player.id, undefined);

  player.then(function () {
      assert.notStrictEqual(player.id, undefined, 'player.id: ' + player.id);

      QUnit.start();
    })
    .catch(onerror)
});

QUnit.asyncTest('Promise', function (assert) {
  var self = this;

  assert.expect(3);

  // Pipeline creation (implicit transaction)
  var pipeline = self.pipeline;

  var player = pipeline.create(QUnit.config.prefix + 'PlayerEndpoint', {
    uri: URL_SMALL
  });
  var recorder = pipeline.create(QUnit.config.prefix + 'RecorderEndpoint', {
    uri: URL_SMALL
  });

  player.connect(recorder)
    .catch(onerror)

  // Atomic operation
  recorder.getMediaPipeline(function (error, rPipeline) {
      if (error) return onerror(error);

      return player.getUri(function (error, uri) {
        if (error) return onerror(error);

        assert.equal(rPipeline.id, pipeline.id);

        // Explicit transaction
        return pipeline.transaction(function () {
            player.getUri(function (error, fUri) {
              assert.equal(fUri, uri, 'URI: ' + fUri);
            });
          },
          // End explicit transaction
          function () {
            assert.notStrictEqual(player.id, undefined,
              'player.id: ' + player.id);

            QUnit.start();
          })
      })
    })
    .catch(onerror)
    // End atomic operation
});

/**
 * Basic pipeline using Transactional API
 */
QUnit.asyncTest('Transactional API', function () {
  var self = this;

  QUnit.expect(1);

  var player;

  self.pipeline.transaction(function () {
      player = this.create(QUnit.config.prefix + 'PlayerEndpoint', {
        uri: URL_SMALL
      });
      var recorder = this.create(QUnit.config.prefix + 'RecorderEndpoint', {
        uri: URL_SMALL
      });

      return player.connect(recorder);
    },
    function (error) {
      QUnit.equal(error, undefined, 'transaction ended');

      if (error) return onerror(error);

      player.release(function (error) {
        if (error) return onerror(error);

        QUnit.start();
      });
    })
    .catch(onerror)
});

/**
 * Basic pipeline using transactional plain API
 */
QUnit.asyncTest('Transactional plain API', function () {
  var self = this;

  QUnit.expect(1);

  var pipeline = self.pipeline;

  var tx = pipeline.beginTransaction();
  var player = pipeline.create(tx, QUnit.config.prefix + 'PlayerEndpoint', {
    uri: URL_SMALL
  });
  var recorder = pipeline.create(tx, QUnit.config.prefix + 'RecorderEndpoint', {
    uri: URL_SMALL
  });

  player.connect(tx, recorder);
  tx.commit(function (error)
    //  pipeline.beginTransaction();
    //    var player  = pipeline.create('PlayerEndpoint', {uri: URL_SMALL});
    //    var httpGet = pipeline.create('HttpGetEndpoint');
    //
    //    player.connect(httpGet);
    //  pipeline.endTransaction(function(error)
    {
      QUnit.equal(error, undefined, 'transaction ended');

      if (error) return onerror(error);

      player.release(function (error) {
        if (error) return onerror(error);

        QUnit.start();
      });
    });
});

/**
 * Create a transaction at beginning and send all commands on it
 */
QUnit.asyncTest('Early transaction', function () {
  var self = this;

  QUnit.expect(0);

  var pipeline = self.pipeline;

  pipeline.transaction(function () {
    var player = pipeline.create(QUnit.config.prefix + 'PlayerEndpoint', {
      uri: URL_SMALL
    });
    var recorder = pipeline.create(QUnit.config.prefix + 'RecorderEndpoint', {
      uri: URL_SMALL
    });

    player.connect(recorder);

    player.release();

    pipeline.release();

    QUnit.start();
  });
});

/**
 * Create object from a declarative description of them
 */
QUnit.asyncTest('Create objects from descriptor object', function(assert)
{
  assert.expect(4);

  var elements =
  [
    {
      type: 'MediaPipeline'
    },
    {
      type: 'PlayerEndpoint',
      params:
      {
        uri: URL_SMALL,
        mediaPipeline: 0
      }
    },
    {
      type: 'RecorderEndpoint',
      params:
      {
        uri: URL_SMALL,
        mediaPipeline: 0
      }
    }
  ]

  this.kurento.create(elements, true, function(error, elements)
  {
    if (error) throw error

    assert.equal(error, undefined)

    var pipeline = elements[0]
    var player   = elements[1]
    var recorder = elements[2]

    assert.notEqual(pipeline, undefined, 'pipeline')
    assert.notEqual(player,   undefined, 'player')
    assert.notEqual(recorder, undefined, 'recorder')

    QUnit.start()
  })
  .catch(onerror)
});


/**
 * Transaction at KurentoClient
 */
/*QUnit.asyncTest('Transactional API', function (assert) {
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
        return player.connect(recorder);
      },
      function (error) {
        assert.equal(error, null, 'transaction');

        if (error) return onerror(error);

        QUnit.start();
      })
    .catch(onerror)
}
);*/
