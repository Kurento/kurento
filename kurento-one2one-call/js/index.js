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


const ws_uri = 'ws://' + location.hostname + ':8888/kurento';

const pubNubOptions =
{
  channel      : 'kurento-videophone-test',
  publish_key  : 'pub-c-25965aa1-5d65-410b-b21d-fd90159adf0e',
  subscribe_key: 'sub-c-cb63e056-f08c-11e3-a672-02ee2ddab7fe'
}


function showSpinner()
{
  for (var i = 0; i < arguments.length; i++)
  {
    arguments[i].poster = 'img/transparent-1px.png';
    arguments[i].style.background = 'center transparent url("img/spinner.gif") no-repeat';
  }
}

function hideSpinner()
{
  for(var i = 0; i < arguments.length; i++)
  {
    arguments[i].poster = 'img/webrtc.png';
    arguments[i].style.background = '';
  }
}


window.addEventListener('load', function()
{
  console = new Console('console', console);
  dragDrop.initElement('videoSmall');

  var videoInput  = document.getElementById('videoInput');
  var videoOutput = document.getElementById('videoOutput');

  document.getElementById('name').focus();

  var softphone = new SoftphonePubnub(ws_uri, videoInput, videoOutput, pubNubOptions);

  softphone.onIncomingCall = function(from, callback)
  {
    if(confirm('User ' + from + ' is calling you. Do you accept the call?'))
    {
      showSpinner(videoInput, videoOutput);

      callback()
    }
  }

  softphone.onStop = function()
  {
    videoInput.src = '';
    videoOutput.src = '';

    hideSpinner(videoInput, videoOutput);
  }


  //
  // UI events
  //

  var btnRegister = document.getElementById('register');
  var btnCall     = document.getElementById('call');
  var terminate   = document.getElementById('terminate');

  var txtName = document.getElementById('name');
  var txtPeer = document.getElementById('peer');


  // Register

  function register()
  {
    var name = txtName.value;

    softphone.register(name);

    console.info("User '" + name + "' registered");
  }

  btnRegister.addEventListener('click', register);

  txtName.addEventListener('keydown', function(event)
  {
    if(event.keyCode == 13) register()
  });


  // Call

  function call()
  {
    showSpinner(videoInput, videoOutput);

    var peer = txtPeer.value;

    softphone.call(peer);

    console.info("Calling user '" + peer + "'");
  }

  btnCall.addEventListener('click', call);

  txtPeer.addEventListener('keydown', function(event)
  {
    if(event.keyCode == 13) call()
  });


  // Terminate

  terminate.addEventListener('click', function()
  {
    softphone.close();
    softphone.onStop();
  });
});


$(document).delegate('*[data-toggle="lightbox"]', 'click', function(event)
{
  event.preventDefault();
  $(this).ekkoLightbox();
});
