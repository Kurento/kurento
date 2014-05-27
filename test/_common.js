/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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

URL_BARCODES         = "https://ci.kurento.com/video/barcodes.webm";
URL_FIWARECUT        = "https://ci.kurento.com/video/fiwarecut.webm";
URL_PLATES           = "https://ci.kurento.com/video/plates.webm";
URL_POINTER_DETECTOR = "https://ci.kurento.com/video/pointerDetector.mp4";
URL_SMALL            = "https://ci.kurento.com/video/small.webm";


/**
 * Set an assert error and re-start the test so it can fail
 */
function onerror(error)
{
  QUnit.ok(false, error.message || error);

  QUnit.start();
};

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

    xhr.addEventListener('load', onsuccess);
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
    clearTimeout(timeout);

    ontimeout(message);
  };

  this.start = function()
  {
    timeout = setTimeout(_ontimeout, delay, 'Time out '+id+' ('+delay+'ms)');
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
      if(error) return console.log(error);

      console.log('XML report saved at '+path);
    });
  }

  // browser - write report to console
  else
    console.log(report.xml);
};


QUnit.config.testTimeout = 60000;


// Tell QUnit what WebSocket servers to use

QUnit.config.urlConfig.push(
{
  id: "ws_uri",
  label: "WebSocket server",
  value:
  {
    'ws://127.0.0.1:8080/thrift/ws/websocket':         'localhost (port 8080)',
    'ws://kms01.kurento.org:8080/thrift/ws/websocket': 'Kurento test server'
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
      ws_uri = 'ws://kms01.kurento.org:8080/thrift/ws/websocket';
    };

    this.kwsMedia = new KwsMedia(ws_uri);

    this.kwsMedia.then(function()
    {
      self.kwsMedia.create('MediaPipeline', function(error, pipeline)
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
    var self = this;

    if(self.pipeline)
      self.pipeline.release(function(error)
      {
        if(error) console.error(error);

        self.kwsMedia.close();
      });
  }
};
