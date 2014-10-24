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
 * {@link HttpEndpoint} test suite.
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
  QUnit = require('qunit-cli');
  QUnit.load();

  kurentoClient = require('..');

  require('./_common');
  require('./_proxy');
};


QUnit.module('GStreamerFilter', lifecycle);

QUnit.asyncTest('End of Stream', function()
{
  var self = this;

  QUnit.expect(5);

  var timeout = new Timeout('"GStreamerFilter:End of Stream"',
                            10 * 1000, onerror);

  function onerror(error)
  {
    timeout.stop();
    _onerror(error);
  };


  self.pipeline.create('PlayerEndpoint', {uri: URL_SMALL}, function(error, player)
  {
    if(error) return onerror(error);

    QUnit.notEqual(player, undefined, 'player');

    self.pipeline.create('GStreamerFilter',
    {command: 'videoflip method=horizontal-flip'},
    function(error, gStreamerFilter)
    {
      if(error) return onerror(error);

      QUnit.notEqual(gStreamerFilter, undefined, 'GStreamerFilter');

      player.connect(gStreamerFilter, function(error)
      {
        QUnit.equal(error, undefined, 'connect');

        if(error) return onerror(error);

        player.on('EndOfStream', function(data)
        {
          QUnit.ok(true, 'EndOfStream');

          timeout.stop();

          QUnit.start();
        });

        player.play(function(error)
        {
          QUnit.equal(error, undefined, 'playing');

          if(error) return onerror(error);

          timeout.start();
        });
      });
    });
  });
});
