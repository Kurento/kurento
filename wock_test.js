if(typeof QUnit == 'undefined')
{
  QUnit = require('qunit-cli');

  wock = require('wock');

//  RTCPeerConnection = require('wrtc').RTCPeerConnection;

  KurentoClient = require('..');
};


function processOffer(peerConnection, offer, onsuccess, onerror)
{
  peerConnection.setRemoteDescription(offer, function()
  {
    peerConnection.createAnswer(function(answer)
    {
      peerConnection.setLocalDescription(answer, function()
      {
        onsuccess(answer);
      },
      onerror);
    },
    onerror);
  },
  onerror);
};

function onerror(error)
{
  console.error(error);
};


/* Proxy */
var objects = {};

function proxy(data)
{
  console.log('< '+data);

  var message = JSON.parse(data);

  var method = message.method;
  var id     = message.id;
  var params = message.params;

  var result = undefined;
  var error  = undefined;

  switch(method)
  {
    case 'create':
    {
      var type = params.type;

      switch(type)
      {
        case 'MediaPipeline':
        {
          objects[id] = 'MediaPipeline';
          result = {id: id};
        }
        break;

        case 'JackVaderFilter':
        case 'PlayerEndpoint':
        case 'WebRtcEndpoint':
        {
          var constructorParams = params.constructorParams;
          var pipeline_id = constructorParams.mediaPipeline;

          var pipeline = objects[pipeline_id];
          if(pipeline)
          {
            objects[id] = params.type;
            result = {id: id};
          }
          else
            error = {message: "Unknown pipeline: "+pipeline_id};
        }
        break;

        default:
          error = {message: "Unknown type: "+type};
      }
    };
    break;

    case 'invoke':
    {
      var operation = params.operation;

      switch(operation)
      {
        case 'connectElements':
          return;
      };
    };
    break;

    default:
      error = {message: "Unknown method: "+method};
  };

  data = JSON.stringify(
  {
    jsonrpc: "2.0",
    id: message.id,
    result: result,
    error: error
  });

  console.log('> '+data);

  this.emit('message', data, {});
};
/* Proxy */


//var WebSocket = wock(proxy);
var WebSocket = require('ws');


KurentoClient(new WebSocket('ws://130.206.81.87/thrift/ws/websocket'),
function(kurento)
{
  // Create pipeline
  kurento.createMediaPipeline(function(error, pipeline)
  {
    if(error) return console.error(error);

    // Create pipeline media elements (endpoints & filters)
    var type = ['PlayerEndpoint', 'JackVaderFilter', 'WebRtcEndpoint'];
    var params = [{uri: "http://localhost:8000/video.avi"}, null, null];

    pipeline.createMediaElement(type, params,
    function(error, mediaElements)
    {
      if(error) return console.error(error);

      var playerEndpoint  = mediaElements[0];
      var jackVaderFilter = mediaElements[1];
      var webRtcEndpoint  = mediaElements[2];

      // Connect media element between them
      pipeline.connect(playerEndpoint, jackVaderFilter, webRtcEndpoint);

      // Subscribe to PlayerEndpoint EOS event
      playerEndpoint.on('EndOfStream', function()
      {
        console.log("EndOfStream");
      });

      // Create a PeerConnection client in the browser
      var peerConnection = new RTCPeerConnection
      (
        {iceServers: [{url: 'stun:stun.l.google.com:19302'}]},
        {optional:   [{DtlsSrtpKeyAgreement: true}]}
      );

      // Connect the pipeline to the PeerConnection client
      webRtcEndpoint.invoke("generateSdpOffer", function(error, offer)
      {
        if(error) return console.error(error);

        processOffer(peerConnection, offer, function(answer)
        {
          webRtcEndpoint.invoke("processSdpAnswer", {answer: answer},
          function(error)
          {
            if(error) return console.error(error);

            var stream = peerConnection.getLocalStreams()[0];

            // Set the stream on the video tag
            var videoOutput = document.getElementById("videoOutput");
                videoOutput.src = URL.createObjectURL(stream);

            // Start player
            playerEndpoint.start();
          });
        },
        onerror);
      });

    });
  });
});
