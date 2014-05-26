function JsonRpcClient(wsUrl, onRequest) {
	this.wsUrl = wsUrl;
	this.onRequest = onRequest;
	this.rpc = new RpcBuilder();
}

JsonRpcClient.prototype.sendRequest = function(method, params, callback) {
	var that = this;
	this.connectIfNecessary(function(ws) {
		var message = that.rpc.encodeJSON(method, params, callback);
		try {
			ws.send(message);
			console.log("--> " + message);
		} catch (e) {
			console.log(e);
		}
	});
};

JsonRpcClient.prototype.connectIfNecessary = function(onOpen) {

	var that = this;

	if (that.ws === undefined) {

		that.ws = new WebSocket(that.wsUrl);

		that.ws.onopen = function() {
			console.log('WebSocket connection stablished');
			onOpen(that.ws);
		};

		// Log errors. TODO export this function to the user
		that.ws.onerror = function(error) {
			console.log('WebSocket Error ' + error);
		};

		// Log messages from the server
		that.ws.onmessage = function(event) {

			console.log('<-- ' + event.data);

			//
			var message = that.rpc.decodeJSON(event.data);

			// Response was processed, do nothing
			if (message == undefined)
				return;

			if (message instanceof RpcBuilder.RpcRequest) {

				// message instanceof RpcBuilder.RpcRequest
				// console.log(message.method);
				// console.log(message.params);

				var transaction = {
					sendResponse : function(result) {
						var response = message.response(null, result);
						console.log("--> " + response);
						that.ws.send(response);
					},
					sendError : function(error) {
						var response = message.response(error, null);
						console.log("--> " + response);
						that.ws.send(response);
					}
				};

				return that.onRequest(transaction, message);

			} else if (message instanceof RpcBuilder.RpcNotification) {

				// Message is an unexpected request, notify error
				if (message.duplicated != undefined)
					return console.error("Unexpected request", message);

				// Message is a notification, process it
				return that.onRequest(message);

			} else {

				console.error("Unrecognized message from server: " + message);
			}
		};
	} else {
		onOpen(this.ws);
	}
};

JsonRpcClient.prototype.close = function() {
	if (this.ws !== undefined) {
		this.ws.close();
	}
};
