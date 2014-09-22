package org.kurento.client.internal.client.operation;

import org.kurento.client.Continuation;
import org.kurento.client.InternalInfoGetter;
import org.kurento.client.KurentoObject;
import org.kurento.client.internal.client.DefaultContinuation;
import org.kurento.client.internal.client.RemoteObject;
import org.kurento.client.internal.client.RemoteObjectFacade;
import org.kurento.client.internal.client.RomManager;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient.RequestAndResponseType;
import org.kurento.jsonrpc.Props;

public class MediaObjectCreationOperation extends Operation {

	public String className;
	public Props constructorParams;
	public KurentoObject mediaObject;

	public MediaObjectCreationOperation(String className,
			Props constructorParams, KurentoObject mediaObject) {
		this.className = className;
		this.constructorParams = constructorParams;
		this.mediaObject = mediaObject;
	}

	public void exec(RomManager manager) {
		RemoteObject remoteObject = manager
				.create(className, constructorParams);
		InternalInfoGetter.setRemoteObject(mediaObject, remoteObject);
	}

	@Override
	public void exec(RomManager manager, final Continuation<Void> cont) {
		manager.create(className, constructorParams,
				new DefaultContinuation<RemoteObjectFacade>(cont) {
					@Override
					public void onSuccess(RemoteObjectFacade remoteObject)
							throws Exception {
						InternalInfoGetter.setRemoteObject(mediaObject,
								remoteObject);
						cont.onSuccess(null);
					}
				});
	}

	@Override
	public RequestAndResponseType createRequest(
			RomClientJsonRpcClient romClientJsonRpcClient) {
		return romClientJsonRpcClient.createCreateRequest(className,
				constructorParams);
	}

	@Override
	public void processResponse(Object response) {
		RemoteObject remoteObject = new RemoteObject((String) response,
				className, manager);
		InternalInfoGetter.setRemoteObject(mediaObject, remoteObject);
	}
}
