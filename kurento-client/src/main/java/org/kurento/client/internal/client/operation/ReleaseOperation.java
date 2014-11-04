package org.kurento.client.internal.client.operation;

import org.kurento.client.KurentoObject;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient.RequestAndResponseType;

public class ReleaseOperation extends Operation {

	private KurentoObject kurentoObject;

	public ReleaseOperation(KurentoObject mediaObject) {
		this.kurentoObject = mediaObject;
	}

	@Override
	public RequestAndResponseType createRequest(
			RomClientJsonRpcClient romClientJsonRpcClient) {

		return romClientJsonRpcClient.createReleaseRequest(
				getObjectRef(kurentoObject));
	}

	@Override
	public void processResponse(Object response) {
		manager.release(getObjectRef(kurentoObject));
	}
}
