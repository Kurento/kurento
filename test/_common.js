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

URL_BARCODES         = "http://files.kurento.org/video/barcodes.webm";
URL_FIWARECUT        = "http://files.kurento.org/video/fiwarecut.webm";
URL_PLATES           = "http://files.kurento.org/video/plates.webm";
URL_POINTER_DETECTOR = "http://files.kurento.org/video/pointerDetector.mp4";
URL_SMALL            = "http://files.kurento.org/video/small.webm";


/**
 * Set an assert error and re-start the test so it can fail
 */
function onerror(error)
{
  QUnit.pushFailure(error.message || error, error.stack);

  QUnit.start();
};

_onerror = onerror;

/**
 * Do an asynchronous HTTP GET request both on Node.js & browser
 */
doGet = function doGet(url, onsuccess, onerror)
{
  // Node.js
  if(typeof XMLHttpRequest == 'undefined')
    require('http').get(url, onsuccess).on('error', onerror);

  // browser
  else
  {
    var xhr = new XMLHttpRequest();

    xhr.open("get", url);
    xhr.send();

    xhr.addEventListener('load', function(event)
    {
      onsuccess(xhr.response);
    });
    xhr.addEventListener('error', onerror);
  };
};


/**
 * Manage timeouts in an object-oriented style
 */
Timeout = function Timeout(id, delay, ontimeout)
{
  if(!(this instanceof Timeout))
    return new Timeout(id, delay, ontimeout);

  var timeout;

  function _ontimeout(message)
  {
    this.stop();

    ontimeout(message);
  };

  this.start = function()
  {
    var delay_factor = delay * Timeout.factor;

    timeout = setTimeout(_ontimeout.bind(this), delay_factor,
                         'Time out '+id+' ('+delay_factor+'ms)');
  };

  this.stop = function()
  {
    clearTimeout(timeout);
  };
};


QUnit.jUnitReport = function(report)
{
  // Node.js - write report to file
  if(typeof window === 'undefined')
  {
    var path = './junitResult.xml';

    require('fs').writeFile(path, report.xml, function(error)
    {
      if(error) return console.error(error);

      console.log('XML report saved at '+path);
    });
  }

  // browser - write report to console
  else
    console.log(report.xml);
};


// Tell QUnit what WebSocket servers to use

QUnit.config.urlConfig.push(
{
  id: "timeout_factor",
  label: "Timeout factor",
  value:
  {
    '0.5' : '0.5x',
    '0.75': '0.75x',
    '1'   : '1x',
    '2'   : '2x',
    '3'   : '3x',
    '5'   : '5x',
    '10'  : '10x'
  },
  tooltip: "Multiply the timeouts window by this factor. Default is 1x"
},
{
  id: "ws_uri",
  label: "WebSocket server",
  value:
  {
    'ws://127.0.0.1:8888/kurento': 'localhost (port 8888)'
  },
  tooltip: "Exec the tests using a real WebSocket server instead of a mock"
});


// Tests lifecycle

lifecycle =
{
  setup: function()
  {
    var self = this;

    var ws_uri = QUnit.config.ws_uri;
    if(ws_uri == undefined)
    {
    //  var WebSocket = wock(proxy);
    //  ws_uri = new WebSocket();
      ws_uri = 'ws://127.0.0.1:8888/kurento';
    };

    Timeout.factor = parseFloat(QUnit.config.timeout_factor) || 1;

    QUnit.config.testTimeout = 30000 * Timeout.factor;

    var options = {request_timeout: 5000 * Timeout.factor};


    this.kurento = new kurentoClient(ws_uri, options);

    this.kurento.then(function()
    {
      this.create('MediaPipeline', function(error, pipeline)
      {
        if(error) return onerror(error);

        self.pipeline = pipeline;

        QUnit.start();
      });
    },
    onerror);

    QUnit.stop();
  },

  teardown: function()
  {
    if(this.pipeline)
      this.pipeline.release(function(error)
      {
        if(error) console.error(error);
      });

    this.kurento.close();
  }
};
