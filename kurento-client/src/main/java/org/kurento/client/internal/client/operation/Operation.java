package org.kurento.client.internal.client.operation;

import org.kurento.client.TransactionExecutionException;
import org.kurento.client.internal.TFutureImpl;
import org.kurento.client.internal.client.RemoteObject;
import org.kurento.client.internal.client.RemoteObjectInvocationHandler;
import org.kurento.client.internal.client.RomManager;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient.RequestAndResponseType;

public abstract class Operation {

	protected RomManager manager;
	protected TFutureImpl<Object> future;

	public abstract RequestAndResponseType createRequest(
			RomClientJsonRpcClient romClientJsonRpcClient);

	public void setManager(RomManager manager) {
		this.manager = manager;
	}

	public TFutureImpl<Object> getFuture() {
		if (future == null) {
			future = new TFutureImpl<>(this);
		}
		return future;
	}

	protected String getObjectRef(Object object) {
		return getRemoteObject(object).getObjectRef();
	}

	protected RemoteObject getRemoteObject(Object object) {
		return RemoteObjectInvocationHandler.getFor(object).getRemoteObject();
	}

	public void rollback(TransactionExecutionException e) {
		if (future != null) {
			if (e != null) {
				future.getFuture().setException(e);
			} else {
				future.getFuture().cancel(true);
			}
		}
	}

	public abstract String getDescription();

	public abstract void processResponse(Object response);

}
