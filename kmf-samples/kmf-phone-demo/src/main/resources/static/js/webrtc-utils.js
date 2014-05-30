function onerror(error)
{
  if(error) console.error(error);
};


function createSendPlayer(sdpOfferReady, callback)
{
  getUserMedia({audio: true, video: true},
  function(stream)
  {
    // Create a PeerConnection client in the browser
    var peerConnection = new RTCPeerConnection(
    {
      iceServers: [{url: 'stun:stun.l.google.com:19302'}]
    },
    {
      optional: [{DtlsSrtpKeyAgreement: true}]
    });

    peerConnection.addStream(stream);

    peerConnection.createOffer(function(offer)
    {
      peerConnection.setLocalDescription(offer, function()
      {
        console.log('offer', offer.sdp);
      },
      callback);
    },
    callback);

    peerConnection.addEventListener('icecandidate', function(event)
    {
      if(event.candidate) return;

      var offer = peerConnection.localDescription;

      console.log('offer+candidates', offer.sdp);

      sdpOfferReady(peerConnection, offer);
    });

    callback(null, stream);
  },
  callback);
}

function createReceivePlayer(peerConnection, sdpAnswer, callback)
{
  var answer = new RTCSessionDescription({type: 'answer', sdp: sdpAnswer});

  console.log('answer', sdpAnswer);

  peerConnection.setRemoteDescription(answer, function()
  {
    var stream = peerConnection.getRemoteStreams()[0];

    callback(null, stream);
  },
  callback);
}
