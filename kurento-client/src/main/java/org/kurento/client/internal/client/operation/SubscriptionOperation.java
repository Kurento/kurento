package org.kurento.client.internal.client.operation;

import org.kurento.client.Continuation;
import org.kurento.client.InternalInfoGetter;
import org.kurento.client.KurentoObject;
import org.kurento.client.internal.client.DefaultContinuation;
import org.kurento.client.internal.client.ListenerSubscriptionImpl;
import org.kurento.client.internal.client.RemoteObject;
import org.kurento.client.internal.client.RemoteObject.RemoteObjectEventListener;
import org.kurento.client.internal.client.RomManager;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient.RequestAndResponseType;

public class SubscriptionOperation extends Operation {

	private KurentoObject object;
	private String eventType;
	private RemoteObjectEventListener listener;
	private ListenerSubscriptionImpl listenerSubscription;

	public SubscriptionOperation(KurentoObject object, String eventType,
			RemoteObjectEventListener listener) {
		this.object = object;
		this.eventType = eventType;
		this.listener = listener;
		this.listenerSubscription = new ListenerSubscriptionImpl(eventType,
				listener);
	}

	@Override
	public void exec(RomManager manager) {

		String subscription = InternalInfoGetter.getRemoteObject(object)
				.addEventListener(eventType, listener).getSubscription();

		listenerSubscription.setSubscription(subscription);
	}

	public ListenerSubscriptionImpl getListenerSubscription() {
		return listenerSubscription;
	}

	@Override
	public void exec(RomManager manager, final Continuation<Void> cont) {

		InternalInfoGetter.getRemoteObject(object).addEventListener(eventType,
				listener,
				new DefaultContinuation<ListenerSubscriptionImpl>(cont) {

					@Override
					public void onSuccess(ListenerSubscriptionImpl result)
							throws Exception {

						listenerSubscription.setSubscription(result
								.getSubscription());
						cont.onSuccess(null);
					}
				});
	}

	@Override
	public RequestAndResponseType createRequest(
			RomClientJsonRpcClient romClientJsonRpcClient) {

		return romClientJsonRpcClient.createSubscribeRequest(InternalInfoGetter
				.getRemoteObject(object).getObjectRef(), eventType);
	}

	@Override
	public void processResponse(Object response) {

		listenerSubscription.setSubscription((String) response);
		((RemoteObject) InternalInfoGetter.getRemoteObject(object))
				.addListener(eventType, listener);
	}

}
