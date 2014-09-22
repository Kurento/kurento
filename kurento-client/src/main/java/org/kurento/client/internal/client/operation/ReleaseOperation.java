package org.kurento.client.internal.client.operation;

import org.kurento.client.Continuation;
import org.kurento.client.InternalInfoGetter;
import org.kurento.client.KurentoObject;
import org.kurento.client.internal.client.RomManager;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient.RequestAndResponseType;

public class ReleaseOperation extends Operation {

	private KurentoObject mediaObject;

	public ReleaseOperation(KurentoObject mediaObject) {
		this.mediaObject = mediaObject;
	}

	@Override
	public void exec(RomManager manager) {
		manager.release(InternalInfoGetter.getRemoteObject(mediaObject)
				.getObjectRef());
	}

	@Override
	public void exec(RomManager manager, final Continuation<Void> cont) {
		manager.release(InternalInfoGetter.getRemoteObject(mediaObject)
				.getObjectRef(), cont);
	}

	@Override
	public RequestAndResponseType createRequest(
			RomClientJsonRpcClient romClientJsonRpcClient) {

		return romClientJsonRpcClient.createReleaseRequest(InternalInfoGetter
				.getRemoteObject(mediaObject).getObjectRef());
	}

	@Override
	public void processResponse(Object response) {
		manager.release(InternalInfoGetter.getRemoteObject(mediaObject)
				.getObjectRef());
	}
}
