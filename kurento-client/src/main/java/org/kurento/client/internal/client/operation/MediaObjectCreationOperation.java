package org.kurento.client.internal.client.operation;

import org.kurento.client.internal.client.RemoteObject;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient.RequestAndResponseType;
import org.kurento.jsonrpc.Props;

public class MediaObjectCreationOperation extends Operation {

	public String className;
	public Props constructorParams;
	private RemoteObject remoteObject;

	public MediaObjectCreationOperation(String className,
			Props constructorParams, RemoteObject remoteObject) {
		this.className = className;
		this.constructorParams = constructorParams;
		this.remoteObject = remoteObject;
	}

	@Override
	public RequestAndResponseType createRequest(
			RomClientJsonRpcClient romClientJsonRpcClient) {
		return romClientJsonRpcClient.createCreateRequest(className,
				constructorParams, true);
	}

	@Override
	public void processResponse(Object response) {
		remoteObject.setCreatedObjectRef((String) response);
	}
}
