/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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


var nodeunit = require('nodeunit');

var RpcBuilder = require("..");


const METHOD = 'test';


exports['encode JsonRPC 2.0'] =
{
  setUp: function(callback)
  {
    this.rpcBuilder = new RpcBuilder();

    callback();
  },

  'notification': function(test)
  {
    test.expect(5);

    var notification = this.rpcBuilder.encode(METHOD);

    test.deepEqual(JSON.parse(notification),
    {
      jsonrpc: '2.0',
      method: METHOD
    });

    // Test notification
    notification = this.rpcBuilder.decode(notification);

    test.ok(notification instanceof RpcBuilder.RpcNotification);
    test.equal(notification.duplicate, undefined);

    test.equal(notification.method, METHOD);
    test.equal(notification.params, undefined);

    test.done();
  },

  'request': function(test)
  {
    test.expect(5);

    var request = this.rpcBuilder.encode(METHOD, function(error, result){});

    test.deepEqual(JSON.parse(request),
    {
      jsonrpc: '2.0',
      method: METHOD,
      id: 0
    });

    // Test request
    request = this.rpcBuilder.decode(request);

    test.ok(request instanceof RpcBuilder.RpcNotification);
    test.notEqual(request.duplicated, false);

    test.equal(request.method, METHOD);
    test.equal(request.params, undefined);

    test.done();
  },

  'duplicated request': function(test)
  {
    test.expect(3);

    var request = this.rpcBuilder.encode(METHOD, function(error, result){});

    test.deepEqual(JSON.parse(request),
    {
      jsonrpc: '2.0',
      method: METHOD,
      id: 0
    });

    // Test request
    var request1 = this.rpcBuilder.decode(request);
    test.notEqual(request1.duplicated, false);

    var request2 = this.rpcBuilder.decode(request);
    test.ok(request2.duplicated);

    test.done();
  },

  'response': function(test)
  {
    test.expect(2);

    var request = this.rpcBuilder.encode(METHOD, function(error, result)
    {
      test.equal(result, null);
    });

    // Compose response manually from the request
    var response = JSON.parse(request);

    delete response.method;
    response.result = null;

    response = JSON.stringify(response);

    // Test response
    response = this.rpcBuilder.decode(response);

    test.equal(response, undefined);

    test.done();
  }
};
