var startSendRecv = kwsUtils.WebRtcPeer.startSendRecv;
var JsonRPC = RpcBuilder.packers.JsonRPC;

const ws_uri = 'ws://demo01.kurento.org:8888/thrift/ws/websocket';


function onerror(error)
{
  if(error) console.error(error);
}


// Process request messages

function onIncommingCall(request)
{
  var params = request.params;

  var from   = params.from;
  var sinkId = params.endpoint;

  createPeer(function(error, kwsMedia, src)
  {
    if(error) return onerror(error);

    var response =
    {
      dest: from,
      endpoint: src.id
    };

    request.reply(null, response);

    // Send our video to the caller
    connectEndpoints(kwsMedia, src, sinkId);
  });
};


// Private functions

function createPeer(callback)
{
  var webRtcPeer = startSendRecv(videoInput, videoOutput, function(offer)
  {
    console.log('Invoking SDP offer callback function');

    KwsMedia(ws_uri, function(error, kwsMedia)
    {
      if(error) return callback(error);

      // Create pipeline
      kwsMedia.create('MediaPipeline', function(error, pipeline)
      {
        if(error) return callback(error);

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

          callback(null, kwsMedia, webRtc);
        });
      });
    },
    onerror);
  });
};

function connectEndpoints(kwsMedia, src, sinkId)
{
  kwsMedia.getMediaobjectById(sinkId, function(error, sink)
  {
    if(error) return onerror(error);

    src.connect(sink, function(error)
    {
      if(error) return onerror(error);

      console.log('loopback established');
    });
  })
};


function SoftphonePubnub(videoInput, videoOutput, options)
{
  self.options = options

  var rpc;


  this.register = function(peerID, options)
  {
    function onRequest(request)
    {
      if(request.params.dest != peerID) return

      switch(request.method)
      {
        case 'call':
          onIncommingCall(request);
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
    createPeer(function(error, kwsMedia, src)
    {
      if(error) return onerror(error);

      var params =
      {
        dest: dest,
        endpoint: src.id
      };

      rpc.encode('call', params, function(error, result)
      {
        if(error) return onerror(error);

        var sinkId = result.endpoint;

        // Send our video to the callee
        connectEndpoints(kwsMedia, src, sinkId);
      });
    });
  };
}
