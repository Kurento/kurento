package org.kurento.client.internal.client.operation;

import org.kurento.client.KurentoObject;
import org.kurento.client.internal.client.ListenerSubscriptionImpl;
import org.kurento.client.internal.client.RemoteObjectEventListener;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient.RequestAndResponseType;

public class SubscriptionOperation extends Operation {

	private KurentoObject kurentoObject;
	private String eventType;
	private RemoteObjectEventListener listener;
	private ListenerSubscriptionImpl listenerSubscription;

	public SubscriptionOperation(KurentoObject object, String eventType,
			RemoteObjectEventListener listener) {
		this.kurentoObject = object;
		this.eventType = eventType;
		this.listener = listener;
		this.listenerSubscription = new ListenerSubscriptionImpl(eventType,
				listener);
	}

	public ListenerSubscriptionImpl getListenerSubscription() {
		return listenerSubscription;
	}

	@Override
	public RequestAndResponseType createRequest(
			RomClientJsonRpcClient romClientJsonRpcClient) {

		return romClientJsonRpcClient.createSubscribeRequest(
				getObjectRef(kurentoObject), eventType);
	}

	@Override
	public void processResponse(Object response) {

		listenerSubscription.setSubscription((String) response);
		getRemoteObject(kurentoObject).addEventListener(eventType, listener);
	}

}
