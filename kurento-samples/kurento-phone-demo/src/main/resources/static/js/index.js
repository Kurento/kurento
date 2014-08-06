const wsUrl = 'ws://' + location.host + '/phone/ws/websocket';

 
window.addEventListener('load', function()
{
  var videoInput  = document.getElementById("videoInput");
  var videoOutput = document.getElementById('videoOutput');

  var softphone = new Softphone(wsUrl, videoInput, videoOutput);

  //
  // UI events
  //

  var btnRegister = document.getElementById('register');
  var btnCall     = document.getElementById('call');

  btnRegister.addEventListener('click', function()
  {
    var name = document.getElementById("name").value;

    softphone.register(name);
  });

  btnCall.addEventListener('click', function()
  {
    var peer = document.getElementById("peer").value;

    softphone.call(peer);
  });
});
