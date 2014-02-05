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


var nock     = require('nock');
var nodeunit = require('nodeunit');

var KwsMedia = require('../lib/index.js');


var MEDIA_DOMAIN = 'http://media.example.com';
var MEDIA_PATH   = '/endpoint';


function setUp()
{
  var mediaServer = nock(MEDIA_DOMAIN)
//  .filteringRequestBody(/.*/, '*')
//  .post(MEDIA_PATH, '*')
//  .post(MEDIA_PATH)
  .get(MEDIA_PATH)
  .reply(201, function(uri, requestBody)
  {
    console.log('uri='+uri);
    console.log('requestBody='+requestBody);

    start();

    mediaServer.done();
    return requestBody;
  });
}


QUnit.module('Kws Media API');


test('calculate', function()
{
  stop();

  setUp();

//  expect(2);

  var uri = MEDIA_DOMAIN + MEDIA_PATH;

  var kwsMedia = new KwsMedia(uri);

//  test.equal(2+2, 4);
//  test.done();
});