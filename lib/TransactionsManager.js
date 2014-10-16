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

var inherits = require('inherits');

var Domain = require('domain').Domain || (function(){
  function FakeDomain(){};
  inherits(FakeDomain, require('events').EventEmitter);
  FakeDomain.prototype.run = function(fn)
  {
    try{fn()}catch(err){this.emit('error', err)};
    return this;
  };
  return FakeDomain;
})();

var Promise = require('es6-promise').Promise;

var promiseCallback = require('promisecallback');


function onerror(error)
{
  this._transactionError = error;
}


function Transaction(manager)
{
  Transaction.super_.call(this);

  this.push           = manager.push.bind(manager);
  this.endTransaction = manager.endTransaction.bind(manager);

  // Errors during transaction execution go to the callback,
  // user will register 'error' event for async errors later
  this.once('error', onerror);
  if(this.enter) this.enter();
}
inherits(Transaction, Domain);


function TransactionsManager(host, commit)
{
  var transactions = [];


  Object.defineProperty(this, 'length', {get: function(){return transactions.length}})


  this.beginTransaction = function()
  {
    var d = new Transaction(this);

    transactions.unshift({d: d, ops: []});

    return d;
  };

  this.endTransaction = function(callback)
  {
    var transaction = transactions.shift();

    var d = transaction.d;

    if(d.exit) d.exit();
    d.removeListener('error', onerror);

    var promise;

    if(d._transactionError)
      promise = Promise.reject(d._transactionError)

    else
    {
      var operations = transaction.ops;

      promise = new Promise(function(resolve, reject)
      {
        function callback(error, result)
        {
          if(error) return reject(error);

          resolve(result)
        }

        commit(operations, callback);
      })
    }

    promise = promiseCallback(promise, callback)

    d.catch = promise.catch.bind(promise);
    d.then  = promise.then.bind(promise);

    delete d.push;
    delete d.endTransaction;

    return d;
  };

  this.transaction = function(func, callback)
  {
    var d = this.beginTransaction();
    d.run(func.bind(host));
    return this.endTransaction(callback);
  };


  this.push = function(data)
  {
    transactions[0].ops.push(data);
  };
};


function transactionOperation(method, params, callback)
{
  var operation =
  {
    method: method,
    params: params,
    callback: callback
  }

  this.push(operation);
};


module.exports = TransactionsManager;
TransactionsManager.Transaction          = Transaction;
TransactionsManager.transactionOperation = transactionOperation;
