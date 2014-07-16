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


const pubNubOptions =
{
  channel      : 'kurento-videophone-test',
  publish_key  : 'pub-c-25965aa1-5d65-410b-b21d-fd90159adf0e',
  subscribe_key: 'sub-c-cb63e056-f08c-11e3-a672-02ee2ddab7fe'
}


window.addEventListener('load', function()
{
  var videoInput  = document.getElementById('videoInput');
  var videoOutput = document.getElementById('videoOutput');

  var softphone = new SoftphonePubnub(videoInput, videoOutput, pubNubOptions);

  //
  // UI events
  //

  var btnRegister = document.getElementById('register');
  var btnCall     = document.getElementById('call');

  btnRegister.addEventListener('click', function()
  {
    var name = document.getElementById('name').value;

    softphone.register(name);
  });

  btnCall.addEventListener('click', function()
  {
    var peer = document.getElementById('peer').value;

    softphone.call(peer);
  });
});
