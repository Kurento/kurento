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

var inherits = require('inherits');

var Domain = require('domain').Domain || (function () {
  function FakeDomain() {};
  inherits(FakeDomain, require('events').EventEmitter);
  FakeDomain.prototype.run = function (fn) {
    try {
      fn()
    } catch (err) {
      this.emit('error', err)
    };
    return this;
  };
  return FakeDomain;
})();

var promiseCallback = require('promisecallback');

function onerror(error) {
  this._transactionError = error;
}

function TransactionNotExecutedException(message) {
  TransactionNotExecutedException.super_.call(this, message);
};
inherits(TransactionNotExecutedException, Error);

function TransactionNotCommitedException(message) {
  TransactionNotCommitedException.super_.call(this, message);
};
inherits(TransactionNotCommitedException, TransactionNotExecutedException);

function TransactionRollbackException(message) {
  TransactionRollbackException.super_.call(this, message);
};
inherits(TransactionRollbackException, TransactionNotExecutedException);

function Transaction(commit) {
  Transaction.super_.call(this);

  var operations = [];

  Object.defineProperty(this, 'length', {
    get: function () {
      return operations.length
    }
  });

  this.push = operations.push.bind(operations);

  Object.defineProperty(this, 'commited', {
    configurable: true,
    value: false
  });

  this.commit = function (callback) {
    if (this.exit) this.exit();
    this.removeListener('error', onerror);

    var promise;

    if (this._transactionError)
      promise = Promise.reject(this._transactionError)

    else {
      operations.forEach(function (operation) {
        var object = operation.params.object;
        if (object && object.transactions) {
          object.transactions.shift();

          if (!object.transactions)
            delete object.transactions;
        }
      });

      var self = this;

      promise = new Promise(function (resolve, reject) {
        function callback(error, result) {
          Object.defineProperty(self, 'commited', {
            value: error == undefined
          });

          if (error) return reject(error);

          resolve(result)
        }

        commit(operations, callback);
      })
    }

    promise = promiseCallback(promise, callback)

    this.catch = promise.catch.bind(promise);
    this.then = promise.then.bind(promise);

    delete this.push;
    delete this.commit;
    delete this.endTransaction;

    return this;
  }

  this.rollback = function (callback) {
    Object.defineProperty(this, 'commited', {
      value: false
    });

    var error = new TransactionRollbackException(
      'Transaction rollback by user');

    // Notify error to all the operations in the transaction
    operations.forEach(function (operation) {
      if (operation.method == 'create')
        operation.params.object.emit('_id', error);

      var callback = operation.callback;
      if (callback instanceof Function)
        callback(error);
    });

    if (callback instanceof Function)
      callback(error);

    return this;
  };

  // Errors during transaction execution go to the callback,
  // user will register 'error' event for async errors later
  this.once('error', onerror);
  if (this.enter) this.enter();
}
inherits(Transaction, Domain);

function TransactionsManager(host, commit) {
  var transactions = [];

  Object.defineProperty(this, 'length', {
    get: function () {
      return transactions.length
    }
  });

  this.beginTransaction = function () {
    var transaction = new Transaction(commit);
    //    transactions.unshift(transaction);
    return transaction;
  };

  this.endTransaction = function (callback) {
    //    return transactions.shift().commit(callback);
  };

  this.transaction = function (func, callback) {
    var transaction = this.beginTransaction();
    transactions.unshift(transaction);

    transaction.run(func.bind(host));

    return transactions.shift().commit(callback);
    //    return this.endTransaction(callback)
  };

  this.push = function (data) {
    transactions[0].push(data);
  }
};

function transactionOperation(method, params, callback) {
  var operation = {
    method: method,
    params: params,
    callback: callback
  }

  var object = params.object;
  if (object) {
    if (object.transactions) {
      object.transactions.unshift(this)
    } else {
      Object.defineProperty(object, 'transactions', {
        configurable: true,
        value: [this]
      });
    }
  }

  this.push(operation);
};

module.exports = TransactionsManager;

TransactionsManager.Transaction = Transaction;
TransactionsManager.transactionOperation = transactionOperation;
TransactionsManager.TransactionNotExecutedException =
  TransactionNotExecutedException;
TransactionsManager.TransactionNotCommitedException =
  TransactionNotCommitedException;
TransactionsManager.TransactionRollbackException = TransactionRollbackException;
