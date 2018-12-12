#!/usr/bin/env node

/*
 * (C) Copyright 2013-2015 Kurento (http://kurento.org/)
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
 * @author Jesús Leganés Combarro "piranna" (piranna@gmail.com)
 * @version 1.0.0
 */

var Docker = require('dockerode');
var minimist = require('minimist');
var spawn = require('child_process').spawn;

var QUnit = require('qunit-cli');

const REPORTS_DIR = 'reports'

function writeReport(ext, data) {
  var path = REPORTS_DIR + '/' + require('../package.json').name + '.' + ext

  require('fs-extra').outputFile(path, data, function (error) {
    if (error) return console.trace(error);

    console.log(ext + ' report saved at ' + path);
  });
}

function fetchReport(type, report) {
  var ext = type
  if (type == 'junit') ext = 'xml'

  report = report[ext]

  writeReport(ext, report)
}

QUnit.jUnitReport = fetchReport.bind(undefined, 'junit')

QUnit.load();

var kurentoClient = require('..');

// Get ws_port
var ws_port = "8888"

// --scope=docker|local --name=kms  --ws_port=XXXX
var argv = minimist(process.argv.slice(2), {
  default: {
    scope: "local",
    name: "kms"
  }
});

if (argv.ws_port == undefined) {
  QUnit.pushFailure("The test needs a ws_port");
}

function isDockerContainer(callback) {
  var isDocker = false;
  var cat = spawn('cat', ['/proc/1/cgroup'])
    .on('error', onerror)

  cat.stdout.on('data', function (data) {
    var lines = data.toString('utf8').split('\n');
    for (var i = 0; i < lines.length; i++) {
      if (lines[i].substr(lines[i].length - 1) != "") {
        if (lines[i].substr(lines[i].length - 1) != "/") {
          isDocker = true;
          callback(isDocker);
          return;
        }
      }
    }
    callback(isDocker);
  });

  cat.stderr.on('data', function (data) {
    // The file is not exist
    callback(isDocker);
  });
}

function getIpDocker(callback) {
  isDockerContainer(function (isDocker) {
    var hostIp;
    if (isDocker) {
      var grep = spawn('grep', ['default']);
      var ip = spawn('ip', ['route']);

      ip.stdout.pipe(grep.stdin);
      grep.stdout.on('data', function (data) {
        callback(data.toString(
          "utf8").split(" ")[2])
      });
    } else {
      var grep = spawn('grep', ['docker']);
      var ip = spawn('ip', ['route']);

      ip.stdout.pipe(grep.stdin);
      grep.stdout.on('data', function (data) {
        var ips = data.toString(
          "utf8").split(" ");
        callback(ips[ips.length - 2])
      });
    }
  })

}

const ARGV = ['-f', './test_reconnect/kurento.conf.json',
  '--gst-debug=Kurento*:5',
  '--modules-config-path=/etc/kurento/modules', '2>&1'
];

var container;
/**
 * Manage timeouts in an object-oriented style
 */
function Timeout(id, delay, ontimeout) {
  if (!(this instanceof Timeout))
    return new Timeout(id, delay, ontimeout);

  var timeout;

  function _ontimeout(message) {
    this.stop();

    ontimeout(message);
  };

  this.start = function () {
    var delay_factor = delay * Timeout.factor;

    timeout = setTimeout(_ontimeout.bind(this), delay_factor,
      'Time out ' + id + ' (' + delay_factor + 'ms)');
  };

  this.stop = function () {
    clearTimeout(timeout);
  };
};

function getOnError(done) {
  return function onerror(error) {
    QUnit.pushFailure(error.message || error, error.stack);

    done();
  };
}

function sleep(seconds) {
  var e = new Date().getTime() + (seconds * 1000);

  while (new Date().getTime() <= e) {}
}

Timeout.factor = parseFloat(QUnit.config.timeout_factor) || 1;

QUnit.config.testTimeout = 310000 * Timeout.factor;

QUnit.module('reconnect', {
  beforeEach: function () {
    var self = this;

    var options = {
      request_timeout: 5000 * Timeout.factor,
      failAfter: 15
    };

    if (argv.scope == "local") {
      var ws_uri = 'ws://127.0.0.1:' + argv.ws_port + '/kurento'

      this.server = spawn('kurento-media-server', ARGV)
        .on('error', onerror)

      this.server.stdout.on('data', function (data) {
        console.log('stdout: ' + data);
      });

      this.server.stderr.on('data', function (data) {
        console.log('stderr: ' + data);
      });

      console.log("Waiting KMS is started... KMS pid:", this.server.pid)

      this.client = kurentoClient(ws_uri, options)
      this.client.create('MediaPipeline', function (error, pipeline) {
        if (error) return onerror(error);

        self.pipeline = pipeline;

        QUnit.start();
      });

      QUnit.stop();
    } else if (argv.scope == "docker") {

      getIpDocker(function (ip) {
        var hostIp = ip;
        console.log("Docker IP:", hostIp);
        docker = new Docker({
          host: hostIp,
          port: 2375
        });
        docker.run('kurento/kurento-media-server-dev:latest', [], [
          process.stdout,
          process.stderr
        ], {
          Tty: false,
          'PortBindings': {
            "8888/tcp": [{
              "HostIp": "",
              "HostPort": argv.ws_port.toString()
            }]
          }
        }, function (err, data, container) {
          if (err) console.error(err);
        }).on('container', function (container_) {
          container = container_;
          container.inspect(function (err, data) {
            container.inspect(function (err, data) {
              container.inspect(function (err, data) {
                var ipDocker = data.NetworkSettings
                  .IPAddress;
                ipDocker = hostIp;
                var ws_uri_docker = 'ws://' +
                  ipDocker +
                  ":" + argv.ws_port + "/kurento";

                self.client = kurentoClient(
                  ws_uri_docker,
                  options)

                self.client.create('MediaPipeline',
                  function (error, pipeline) {
                    if (error) return onerror(error);
                    self.pipeline = pipeline;
                    QUnit.start();
                  });

              })
            })
          })
        });
      });
      QUnit.stop();
    }
  },

  afterEach: function () {
    this.client.close();
    if (argv.scope == "local") {
      this.server.kill()
    } else if (argv.scope == "docker") {
      QUnit.stop();
      container.stop(function (error, data) {
        console.log("Container KMS stopped.")
        container.remove(function (error, data) {
          console.log("Container KMS removed.")
          QUnit.start();
        })
      });
    }
  }
});

/**
 * restart the MediaServer and keep the session
 */
QUnit.test('MediaServer restarted', function (assert) {
  var self = this;

  var done = assert.async()
  var onerror = getOnError(done)

  var client = self.client
  var pipeline = self.pipeline

  var sessionId = client.sessionId;

  // restart MediaServer
  if (argv.scope == "local") {
    QUnit.expect(4);

    self.server.kill();
    self.server.on('exit', function (code, signal) {
      assert.equal(code, 0, 'MediaServer killed');

      client._resetCache()

      self.server = spawn('kurento-media-server', ARGV)
        .on('error', onerror)

      self.server.stdout.on('data', function (data) {
        console.log('stdout: ' + data);
      });

      self.server.stderr.on('data', function (data) {
        console.log('stderr: ' + data);
      });

      console.log("Waiting KMS is started again... KMS pid:", self.server
        .pid)

      var grep = spawn('grep', ['kurento']);
      var ps = spawn('ps', ['aux']);

      ps.stdout.pipe(grep.stdin);

      grep.stdout.on('data', function (data) {
        console.log("ps aux | grep kurento =>", data.toString(
          "utf8"));
      });

      client.getMediaobjectById(pipeline.id, function (error,
        mediaObject) {
        console.log("Info on client.getMediaObjectById: error->",
          error);
        console.log(
          "Info on client.getMediaObjectById: client.sessionId(",
          client.sessionId, ") == ", sessionId);
        assert.notEqual(error, undefined);
        assert.strictEqual(error.code, 40101);

        assert.strictEqual(client.sessionId, sessionId);

        done();
      })
    })
  } else if (argv.scope == "docker") {
    QUnit.expect(3);
    container.stop(function (error, data) {
      //container.remove(function (error, data) {

      docker.run('kurento/kurento-media-server-dev:latest', [], [
          process.stdout,
          process.stderr
        ], {
          Tty: false,
          'PortBindings': {
            "8888/tcp": [{
              "HostIp": "",
              "HostPort": argv.ws_port.toString()
            }]
          }
        },
        function (err, data, container) {
          if (err) console.error(err);
        }).on('container', function (container_) {
        container = container_;
        container.inspect(function (err, data) {
          container.inspect(function (err, data) {
            container.inspect(function (err, data) {

              client._resetCache()

              client.getMediaobjectById(pipeline.id,
                function (
                  error,
                  mediaObject) {
                  console.log(
                    "Info on client.getMediaObjectById: error->",
                    error);
                  console.log(
                    "Info on client.getMediaObjectById: client.sessionId(",
                    client.sessionId, ") == ",
                    sessionId);
                  assert.notEqual(error, undefined);
                  assert.strictEqual(error.code,
                    40101);

                  assert.strictEqual(client.sessionId,
                    sessionId);
                  done();
                })
            })
          })
        })
      });
      //})
    });
  }
});

/**
 * All objects are Invalid after the MediaServer got down
 */
QUnit.test('MediaServer closed, client disconnected', function (assert) {
  var self = this;

  QUnit.expect(2);

  var done = assert.async()
  var onerror = getOnError(done)

  var client = self.client
  var pipeline = self.pipeline

  // stop MediaServer
  if (argv.scope == "local") {
    self.server.kill();
    self.server.on('exit', function (code, signal) {
      console.log("Server was killed");
      client.once('disconnect', function (error) {
        console.log("Client disconnected");
        assert.notEqual(error, undefined);

        assert.throws(function () {
            client.sessionId
          },
          new SyntaxError('Client has been disconnected'));

        done();
      });
    })
  } else if (argv.scope == "docker") {
    container.stop(function (error, data) {
      console.log("Container KMS stopped.")
      container.remove(function (error, data) {
        console.log("Container KMS removed", client)
        client.once('disconnect', function (error) {
          console.log("Client disconnected");
          assert.notEqual(error, undefined);

          assert.throws(function () {
              client.sessionId
            },
            new SyntaxError('Client has been disconnected')
          );

          done();
        });
      })
    })
  }
});
