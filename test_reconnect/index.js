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

if(typeof QUnit == 'undefined')
{
  var spawn = require('child_process').spawn;

  QUnit = require('qunit-cli');
  QUnit.load();

  kurentoClient = require('..');
};


URL_SMALL = "http://files.kurento.org/video/small.webm";

const args = ['--gst-debug-no-color','-f','./kurento.conf.json'];


/**
 * Set an assert error and re-start the test so it can fail
 */
function onerror(error)
{
  QUnit.pushFailure(error.message || error, error.stack);

//  client.close();

  QUnit.start();
};


QUnit.module('reconnect');

/**
 * restart the MediaServer and keep working
 */
QUnit.asyncTest('MediaServer restarted', function()
{
  var self = this;

  QUnit.expect(6);


  var child = spawn('kurento-media-server', args)

  // First connection
  kurentoClient('ws://127.0.0.1:8889/kurento')
  .then(function createPipeline(client)
  {
    //Create pipeline
    client.create('MediaPipeline', function(error, pipeline)
    {
      if(error) return onerror(error);

      var sessionId = client.sessionId;

      pipeline.create('PlayerEndpoint', {uri: URL_SMALL}, function(error, player)
      {
        if(error) return onerror(error);

        pipeline.create('HttpGetEndpoint', function(error, httpGet)
        {
          if(error) return onerror(error);

          player.connect(httpGet, function(error)
          {
            if(error) return onerror(error);

            // restart MediaServer
            child.kill();
            child = spawn('kurento-media-server', args)

            QUnit.strictEqual(client.sessionId, sessionId);

            client.getMediaobjectById([pipeline.id, player.id, httpGet.id],
            function(error, mediaObjects)
            {
              if(error) return onerror(error);

              QUnit.strictEqual(mediaObjects.length, 3);

              QUnit.strictEqual(mediaObjects[0], pipeline);
              QUnit.strictEqual(mediaObjects[1], player);
              QUnit.strictEqual(mediaObjects[2], httpGet);

              player.play(function(error)
              {
                QUnit.notStrictEqual(error, undefined);

                client.close();
                child.kill();

                QUnit.start();
              })
            })
          });
        });
      });
    });
  },
  onerror);
});

/**
 * Stop the server, start it later and keep using the same client
 */
QUnit.asyncTest('Keep using client after MediaServer restart', function()
{
  var self = this;

  QUnit.expect(1);


  var child = spawn('kurento-media-server', args)

  // First connection
  kurentoClient('ws://127.0.0.1:8889/kurento')
  .then(function createPipeline(client)
  {
    // stop MediaServer
    child.kill();

    client.once('disconnect', function()
    {
      // start MediaServer
      child = spawn('kurento-media-server', args)

      //Create pipeline
      client.create('MediaPipeline', function(error, pipeline)
      {
        if(error) return onerror(error);

        pipeline.create('PlayerEndpoint', {uri: URL_SMALL},
        function(error, player)
        {
          if(error) return onerror(error);

          pipeline.create('HttpGetEndpoint', function(error, httpGet)
          {
            if(error) return onerror(error);

            player.connect(httpGet, function(error)
            {
              if(error) return onerror(error);

              player.play(function(error)
              {
                QUnit.equal(error, undefined);

                client.close();
                child.kill();

                QUnit.start();
              });
            });
          });
        });
      });
    });
  },
  onerror);
});

/**
 * Close the connection and keep working
 */
QUnit.asyncTest('Network error', function()
{
  var self = this;

  QUnit.expect(6);


  // First connection
  kurentoClient('ws://127.0.0.1:8888/kurento')
  .then(function createPipeline(client)
  {
    var sessionId = client.sessionId;

    //Create pipeline
    client.create('MediaPipeline', function(error, pipeline)
    {
      if(error) return onerror(error);

      pipeline.create('PlayerEndpoint', {uri: URL_SMALL}, function(error, player)
      {
        if(error) return onerror(error);

        pipeline.create('HttpGetEndpoint', function(error, httpGet)
        {
          if(error) return onerror(error);

          player.connect(httpGet, function(error)
          {
            if(error) return onerror(error);

            // End connection and wait for a new one
            var old_connection = client._re._connection;
            old_connection.end();

            client._re.once('connect', function(con)
            {
              QUnit.notStrictEqual(con, old_connection);

              QUnit.strictEqual(client.sessionId, sessionId,
                'sessionId='+client.sessionId);

              client.getMediaobjectById([pipeline.id, player.id, httpGet.id],
              function(error, mediaObjects)
              {
                if(error) return onerror(error);

                QUnit.strictEqual(mediaObjects.length, 3);

                QUnit.strictEqual(mediaObjects[0], pipeline);
                QUnit.strictEqual(mediaObjects[1], player);
                QUnit.strictEqual(mediaObjects[2], httpGet);

                player.play(function(error)
                {
                  if(error) return onerror(error);

                  client.close();

                  QUnit.start();
                })
              })
            });
          });
        });
      });
    });
  },
  onerror);
});
