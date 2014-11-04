package org.kurento.client.internal.client.operation;

import java.util.concurrent.Future;

import org.kurento.client.internal.client.RemoteObject;
import org.kurento.client.internal.client.RemoteObjectInvocationHandler;
import org.kurento.client.internal.client.RomManager;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient.RequestAndResponseType;

import com.google.common.util.concurrent.SettableFuture;

public abstract class Operation {

	protected RomManager manager;

	public void setManager(RomManager manager) {
		this.manager = manager;
	}

	protected SettableFuture<Object> future = SettableFuture.create();

	public abstract RequestAndResponseType createRequest(
			RomClientJsonRpcClient romClientJsonRpcClient);

	public abstract void processResponse(Object response);

	public Future<Object> getFuture() {
		return future;
	}

	protected String getObjectRef(Object object) {
		return getRemoteObject(object).getObjectRef();
	}

	protected RemoteObject getRemoteObject(Object object) {
		return RemoteObjectInvocationHandler.getFor(object).getRemoteObject();
	}

}
