function Softphone(wsUrl, videoInput, videoOutput)
{
  var client = new JsonRpcClient(wsUrl, onRequest);

  var localPeerConnection;


  // Process request messages

  function onRequest(request)
  {
    switch(request.method)
    {
      case 'incommingCall':
        onIncommingCall(request);
      break;

      case 'startCommunication':
        onStartCommunication(request);
      break;

      default:
        console.error('Unrecognized request', request);
    }
  };

  function onIncommingCall(request)
  {
    function sdpOfferReady(peerConnection, offer)
    {
      localPeerConnection = peerConnection;

      var response =
      {
        callResponse: 'Accept',
        sdpOffer: offer.sdp
      };

      request.reply(null, response);
    };

    createSendPlayer(sdpOfferReady, setVideoInput);
  };

  function onStartCommunication(request)
  {
    var sdpAnswer = request.params.sdpAnswer;

    createReceivePlayer(localPeerConnection, sdpAnswer, setVideoOutput);

    request.reply(null, {});
  };


  // Set videos

  function setVideoInput(error, stream)
  {
    if(error) return onerror(error);

    // Set the stream on the video tag
    videoInput.src = URL.createObjectURL(stream);
  };

  function setVideoOutput(error, stream)
  {
    if(error) return onerror(error);

    // Set the stream on the video tag
    videoOutput.src = URL.createObjectURL(stream);
  };


  // Public API

  this.register = function(name)
  {
    var params = {name: name};

    client.sendRequest('register', params, function(error)
    {
      if(error) return onerror(error);

      console.log('registered');
    });
  };

  this.call = function(peer)
  {
    function sdpOfferReady(peerConnection, offer)
    {
      var params = {callTo: peer, sdpOffer: offer.sdp};

      client.sendRequest('call', params, function(error, result)
      {
        if(error) return onerror(error);

        createReceivePlayer(peerConnection, result.sdpAnswer, setVideoOutput);
      });	
    };

    createSendPlayer(sdpOfferReady, setVideoInput);
  };
};
