/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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

var objects = {};

function proxy(data) {
  console.log('< ' + data);

  var message = JSON.parse(data);

  var method = message.method;
  var id = message.id;
  var params = message.params;

  var result = undefined;
  var error = undefined;

  function method_create() {
    var type = params.type;

    switch (type) {
    case 'PlayerEndpoint':
    case 'ZBarFilter':
      {
        var constructorParams = params.constructorParams;
        var pipeline_id = constructorParams.mediaPipeline;

        var pipeline = objects[pipeline_id];
        if (pipeline == undefined) {
          error = {
            message: "Unknown pipeline: " + pipeline_id
          };
          break;
        };
      };

    case 'MediaPipeline':
      {
        objects[id] = type;
        result = {
          value: id
        };
      }
      break;

    default:
      error = {
        message: "Unknown type: " + type
      };
    }
  };

  if (method == 'create')
    method_create();

  else {
    var id = params.id;

    var object = objects[id];
    if (object == undefined)
      error = {
        message: "Unknown object: " + id
      };

    else
      switch (method) {
      case 'invoke':
        {
          var operation = params.operation;

          switch (operation) {
          case 'connect':
          case 'play':
            result = {};

          default:
            error = {
              message: "Unknown operation: " + operation
            };
          };
        };
        break;

      case 'release':
        {
          result = {};
        };
        break;

      case 'subscribe':
        {
          var type = params.type;

          switch (type) {
          case 'CodeFound':
            result = {};

          default:
            error = {
              message: "Unknown event type: " + type
            };
          };
        };
        break;

      case 'unsubscribe':
        {
          result = {};
        };
        break;

      default:
        error = {
          message: "Unknown method: " + method
        };
      };
  }

  data = JSON.stringify({
    jsonrpc: "2.0",
    id: message.id,
    result: result,
    error: error
  });

  console.log('> ' + data);

  this.emit('message', data, {});
};
