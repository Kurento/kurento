package org.kurento.client.internal.client;

import java.lang.reflect.Type;

import org.kurento.client.Continuation;
import org.kurento.client.KurentoObject;
import org.kurento.client.TransactionNotExecutedException;
import org.kurento.client.internal.TransactionImpl;
import org.kurento.client.internal.client.RemoteObject.RemoteObjectEventListener;
import org.kurento.client.internal.client.operation.SubscriptionOperation;
import org.kurento.jsonrpc.Props;

public class NonCommitedRemoteObject implements RemoteObjectFacade {

	private TransactionImpl tx;
	private String objectRef;

	public NonCommitedRemoteObject(String objectId, TransactionImpl tx) {
		this.objectRef = objectId;
		this.tx = tx;
	}

	public NonCommitedRemoteObject() {
		this.objectRef = "XX";
	}

	@Override
	public KurentoObject getPublicObject() {
		return null;
	}

	@Override
	public void setPublicObject(KurentoObject mediaObject) {

	}

	@SuppressWarnings("unchecked")
	@Override
	public <E> E invoke(String method, Props params, Class<E> clazz) {
		return (E) invoke(method, params, (Type) clazz);
	}

	@Override
	public Object invoke(String method, Props params, Type returnType) {
		throw new TransactionNotExecutedException();
	}

	@Override
	public void release() {
		throwException();
	}

	@Override
	public ListenerSubscriptionImpl addEventListener(String eventType,
			RemoteObjectEventListener listener) {

		SubscriptionOperation op = new SubscriptionOperation(
				(KurentoObject) this.getPublicObject(), eventType, listener);

		tx.addOperation(op);

		return op.getListenerSubscription();
	}

	// TODO Review if this implementation is correct. I think we should allow
	// event registering in async mode when non ready, but this implementation
	// doesn't allow it
	@Override
	public void addEventListener(String eventType,
			RemoteObjectEventListener listener,
			Continuation<ListenerSubscriptionImpl> cont) {
		try {
			cont.onError(new TransactionNotExecutedException());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getObjectRef() {
		return objectRef;
	}

	@Override
	public void fireEvent(String type, Props data) {
		throwException();
	}

	@Override
	public String getType() {
		throwException();
		return null;
	}

	@Override
	public RomManager getRomManager() {
		throwException();
		return null;
	}

	private void throwException() {
		throw new TransactionNotExecutedException();
	}

	@Override
	public void release(Continuation<Void> continuation) {
		try {
			continuation.onError(new TransactionNotExecutedException());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
