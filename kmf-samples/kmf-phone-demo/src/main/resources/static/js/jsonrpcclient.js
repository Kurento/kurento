function JsonRpcClient(wsUrl, onRequest)
{
  var ws = new WebSocket(wsUrl);

	var rpc = new RpcBuilder(RpcBuilder.packers.JsonRPC, ws);

  // Log errors. TODO export this function to the user
  ws.addEventListener('error', function(error)
  {
    console.log('WebSocket Error ' + error);
  });

  // Log messages from the server
  ws.addEventListener('message', function(event)
  {
    var message = rpc.decode(event.data);
    if(message)
      onRequest(message);
  });

  this.close       = rpc.close.bind(rpc);
  this.sendRequest = rpc.encode.bind(rpc);
}
