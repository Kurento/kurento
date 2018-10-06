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

var Docker = require('dockerode');
var minimist = require('minimist');
var spawn = require('child_process').spawn;

URL_VIDEO_FILES = "http://files.openvidu.io/video/";
URL_BARCODES = URL_VIDEO_FILES + "filter/barcodes.webm";
URL_FIWARECUT = URL_VIDEO_FILES + "filter/fiwarecut.webm";
URL_PLATES = URL_VIDEO_FILES + "filter/plates.webm";
URL_POINTER_DETECTOR = URL_VIDEO_FILES + "filter/pointerDetector.mp4";
URL_SMALL = URL_VIDEO_FILES + "format/small.webm";

/**
 * Set an assert error and re-start the test so it can fail
 */
function onerror(error) {
  if (error)
    QUnit.pushFailure(error.message || error, error.stack);

  QUnit.start();
};

_onerror = onerror;

/**
 * Do an asynchronous HTTP GET request both on Node.js & browser
 */
doGet = function doGet(url, onsuccess, onerror) {
  // Node.js
  if (typeof XMLHttpRequest == 'undefined')
    require('http').get(url, onsuccess).on('error', onerror);

  // browser
  else {
    var xhr = new XMLHttpRequest();

    xhr.open("get", url);
    xhr.send();

    xhr.addEventListener('load', function (event) {
      onsuccess(xhr.response);
    });
    xhr.addEventListener('error', onerror);
  };
};

/**
 * Manage timeouts in an object-oriented style
 */
Timeout = function Timeout(id, delay, ontimeout) {
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

const REPORTS_DIR = 'reports'

function writeReport(ext, data) {
  var path = REPORTS_DIR + '/' + QUnit.config.prefix + require(
    '../package.json').name + '.' + ext

  require('fs-extra').outputFile(path, data, function (error) {
    if (error) return console.trace(error);

    console.log(ext + ' report saved at ' + path);
  });
}

function fetchReport(type, report) {
  var ext = type
  if (type == 'junit') ext = 'xml'

  report = report[ext]

  // Node.js - write report to file
  if (typeof window === 'undefined')
    writeReport(ext, report)

  // browser - write report to console
  else {
    var textarea = document.getElementById(type);

    textarea.value = report;
    textarea.style.height = textarea.scrollHeight + "px";
    textarea.style.visibility = "visible";
  }
}

// Check if use docker or local

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

function getopts(args, opts) {
  var result = opts.default || {};
  args.replace(
    new RegExp("([^?=&]+)(=([^&]*))?", "g"),
    function ($0, $1, $2, $3) {
      result[$1] = decodeURI($3);
    });

  return result;
};

// Only process arguments for browsers
try {
  var args = getopts(location.search, {
    default: {

    }
  });
} catch (e) {}

QUnit.jUnitReport = fetchReport.bind(undefined, 'junit')
QUnit.lcovReport = fetchReport.bind(undefined, 'lcov')

// Tell QUnit what WebSocket servers to use

QUnit.config.urlConfig.push({
  id: "timeout_factor",
  label: "Timeout factor",
  value: {
    '0.5': '0.5x',
    '0.75': '0.75x',
    '1': '1x',
    '2': '2x',
    '3': '3x',
    '5': '5x',
    '10': '10x'
  },
  tooltip: "Multiply the timeouts window by this factor. Default is 1x"
}, {
  id: "ws_uri",
  label: "WebSocket server",
  value: {
    'ws://127.0.0.1:8888/kurento': 'localhost (port 8888)'
  },
  tooltip: "Exec the tests using a real WebSocket server instead of a mock"
});

var ws_uri;

if (args != undefined && args.ws_uri != undefined) {
  ws_uri = args.ws_uri;
} else {
  ws_uri = QUnit.config.ws_uri;
}
var ws_port = QUnit.config.ws_port;
var scope = QUnit.config.scope;
var container;

// Tests lifecycle

lifecycle = {
  setup: function () {
    var self = this;
    if (ws_uri == undefined) {
      //  var WebSocket = wock(proxy);
      //  ws_uri = new WebSocket();
      ws_uri = 'ws://127.0.0.1:8888/kurento';
    };

    if (ws_port == undefined) {
      ws_port = "8888";
    }

    if (scope == undefined) {
      scope = "local";
    }

    if (scope == "local") {
      Timeout.factor = parseFloat(QUnit.config.timeout_factor) || 1;

      QUnit.config.testTimeout = 30000 * Timeout.factor;

      var options = {
        request_timeout: 5000 * Timeout.factor
      };

      this.kurento = new kurentoClient(ws_uri, options);

      this.kurento.then(function () {
          this.create('MediaPipeline', function (error, pipeline) {
            if (error) return onerror(error);

            self.pipeline = pipeline;

            QUnit.start();
          });
        },
        onerror);
    } else if (scope == "docker") {
      getIpDocker(function (ip) {
        var hostIp = ip;
        console.log("Docker IP:", hostIp);
        docker = new Docker();
        docker.run('kurento/kurento-media-server-dev:latest', [], [
          process.stdout,
          process.stderr
        ], {
          Tty: false,
          'PortBindings': {
            "8888/tcp": [{
              "HostIp": "",
              "HostPort": ws_port.toString()
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
                ws_uri = 'ws://' +
                  ipDocker +
                  ":" + ws_port + "/kurento";
                Timeout.factor = parseFloat(QUnit.config
                  .timeout_factor) || 1;

                QUnit.config.testTimeout = 30000 *
                  Timeout.factor;

                var options = {
                  request_timeout: 5000 * Timeout.factor
                };

                self.kurento = new kurentoClient(ws_uri,
                  options);
                self.kurento.then(function () {
                    this.create('MediaPipeline',
                      function (error, pipeline) {
                        if (error) return onerror(
                          error);

                        self.pipeline = pipeline;

                        QUnit.start();
                      });
                  },
                  onerror);

              })
            })
          })
        });
      });
    }

    QUnit.stop();
  },

  teardown: function () {
    if (scope == "docker") {
      this.kurento.close();
      QUnit.stop();
      container.stop(function (error, data) {
        console.log("Container KMS stopped.")
        container.remove(function (error, data) {
          console.log("Container KMS removed.")
          QUnit.start();
        })
      });
    } else {
      if (this.pipeline)
        this.pipeline.release(function (error) {
          if (error) console.error(error);
        });
      this.kurento.close();
    }
  }
};
