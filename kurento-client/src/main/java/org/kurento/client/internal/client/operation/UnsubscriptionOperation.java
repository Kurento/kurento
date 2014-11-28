package org.kurento.client.internal.client.operation;

import org.kurento.client.KurentoObject;
import org.kurento.client.internal.client.ListenerSubscriptionImpl;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient.RequestAndResponseType;

public class UnsubscriptionOperation extends Operation {
	
	private KurentoObject kurentoObject;
	private ListenerSubscriptionImpl listenerSubscription;
	
	public UnsubscriptionOperation(KurentoObject kurentoObject,
			ListenerSubscriptionImpl listenerSubscription) {
		this.listenerSubscription = listenerSubscription;
		this.kurentoObject = kurentoObject;
	}
	
	public ListenerSubscriptionImpl getListenerSubscription() {
		return listenerSubscription;
	}
	
	public KurentoObject getKurentoObject() {
		return kurentoObject;
	}
	
	@Override
	public RequestAndResponseType createRequest(
			RomClientJsonRpcClient romClientJsonRpcClient) {
		
		return romClientJsonRpcClient.createUnsubscribeRequest(
				getObjectRef(kurentoObject),
				listenerSubscription.getSubscriptionId());
	}
	
	@Override
	public void processResponse(Object response) {
		// There is nothing to do here.
	}
	
	@Override
	public String getDescription() {
		return "Event " + listenerSubscription.getType() + " unsubscription";
	}
	
}
