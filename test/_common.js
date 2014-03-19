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
URL_SMALL            = "https://ci.kurento.com/video/small.webm";
URL_POINTER_DETECTOR = "https://ci.kurento.com/video/pointerDetector.mp4";


function onerror(error)
{
  QUnit.ok(false, error.message || error);

  QUnit.start();
};


QUnit.config.urlConfig.push(
{
  id: "ws_uri",
  label: "WebSocket server",
  value:
  {
    'ws://130.206.81.87/thrift/ws/websocket':  'Kurento demo server',
    'ws://127.0.0.1:7788/thrift/ws/websocket': 'localhost (puerto 7788)'
  },
  tooltip: "Exec the tests using a real WebSocket server instead of a mock"
});


lifecycle =
{
  setup: function()
  {
    var ws_uri = QUnit.config.ws_uri;
    if(ws_uri == undefined)
    {
    //  var WebSocket = wock(proxy);
    //  ws_uri = new WebSocket();
      ws_uri = 'ws://130.206.81.87/thrift/ws/websocket';
    };

    kwsMedia = new KwsMedia(ws_uri);
  },

  teardown: function()
  {
    kwsMedia.close();
  }
};
