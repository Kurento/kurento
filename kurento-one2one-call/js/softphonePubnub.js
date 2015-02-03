var startSendRecv = kurentoUtils.WebRtcPeer.startSendRecv;
var JsonRPC = RpcBuilder.packers.JsonRPC;

var _pipeline;

function onerror(error)
{
  if(error) console.error(error);
}


// Private functions

function createPeer(ws_uri, callback, pipeline)
{
  var webRtcPeer = startSendRecv(videoInput, videoOutput, function(offer)
  {
    console.log('Invoking SDP offer callback function');

    kurentoClient(ws_uri, function(error, client)
    {
      if(error) return callback(error);

      //Create pipeline
      if (pipeline == undefined) {
        client.create('MediaPipeline', function(error, pipeline)
        {
          if(error) return callback(error);

          _pipeline = pipeline;
          // Create pipeline media elements
          pipeline.create('WebRtcEndpoint', function(error, webRtc)
          {
            if(error) return callback(error);

            // Connect the pipeline to the WebRtcPeer client
            webRtc.processOffer(offer, function(error, answer)
            {
              if(error) return callback(error);

              console.log('answer', answer);

              webRtcPeer.processSdpAnswer(answer);
            });

            callback(null, client, webRtc);
          });
        });
      } else {
        client.getMediaobjectById(pipeline, function(error, aux)
        {
          if(error) return callback(error);
          aux.create('WebRtcEndpoint', function(error, webRtc)
          {
            if(error) return callback(error);

            // Connect the pipeline to the WebRtcPeer client
            webRtc.processOffer(offer, function(error, answer)
            {
              if(error) return callback(error);

              console.log('answer', answer);
              webRtcPeer.processSdpAnswer(answer);
            });

            callback(null, client, webRtc);
          });
        });
      }
    },
    onerror);
  });

  return webRtcPeer;
};

function connectEndpoints(client, src, sinkId)
{
  client.getMediaobjectById(sinkId, function(error, sink)
  {
    if(error) return onerror(error);

    src.connect(sink, function(error)
    {
      if(error) return onerror(error);

      console.log('loopback established');
    });
  })
};


function SoftphonePubnub(ws_uri, videoInput, videoOutput, options)
{
  if(!(this instanceof SoftphonePubnub))
    return new SoftphonePubnub(ws_uri, videoInput, videoOutput, options);

  var self = this;

  var rpc;


  this.options = options;


  var webRtcPeer;

  // Process request messages

  function onIncomingCall(request)
  {
    if(!self.onIncomingCall)
      throw new SyntaxError('onIncomingCall is not defined')

    var params = request.params;

    var from   = params.from;
    var sinkId = params.endpoint;
    var pipeline = params.pipeline;

    self.onIncomingCall(from, function()
    {
      webRtcPeer = createPeer(ws_uri, function(error, client, src)
      {
        if(error) return onerror(error);

        var response =
        {
          dest: from,
          endpoint: src.id
        };

        request.reply(null, response);

        // Send our video to the caller
        connectEndpoints(client, src, sinkId);
      }, pipeline);
    })
  };


  this.register = function(peerID, options)
  {
    function onRequest(request)
    {
      if(request.params.dest != peerID) return

      switch(request.method)
      {
        case 'call':
          onIncomingCall(request);
        break;

        case 'stop':
          self.onStop();
        break;

        default:
          console.error('Unrecognized request', request);
      }
    };


    options = options || self.options

    var channel = options.channel

    var rpcOptions =
    {
      peerID: peerID,
      request_timeout: 10*1000
    };

    rpc = new RpcBuilder(JsonRPC, rpcOptions);

    var pubnub = PUBNUB.init(options);

    pubnub.subscribe(
    {
      channel: channel,
      message: function(message)
      {
        var request = rpc.decode(message);
        if(request)
          onRequest(request);
      }
    });

    rpc.transport = function(message)
    {
      pubnub.publish(
      {
        channel: channel,
        message: message
      });
    }
  };

  this.call = function(dest)
  {
    webRtcPeer = createPeer(ws_uri, function(error, client, src)
    {
      if(error) return onerror(error);

      var params =
      {
        dest: dest,
        pipeline : _pipeline.id,
        endpoint: src.id
      };

      rpc.encode('call', params, function(error, result)
      {
        if(error) return onerror(error);

        var sinkId = result.endpoint;

        // Send our video to the callee
        connectEndpoints(client, src, sinkId);
      });
    });

    self.close = function()
    {
      if(webRtcPeer)
      {
        rpc.encode('stop', {dest: dest});

        webRtcPeer.dispose();
        webRtcPeer = null
      }
    };
  };
}
