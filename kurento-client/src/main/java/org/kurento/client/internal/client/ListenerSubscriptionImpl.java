package org.kurento.client.internal.client;

import org.kurento.client.ListenerSubscription;
import org.kurento.client.internal.client.RemoteObject.RemoteObjectEventListener;

public class ListenerSubscriptionImpl implements ListenerSubscription {

	private String subscriptionId;
	private String type;
	private RemoteObjectEventListener listener;

	public ListenerSubscriptionImpl(String subscription, String type,
			RemoteObjectEventListener listener) {
		this.subscriptionId = subscription;
		this.type = type;
		this.listener = listener;
	}

	public ListenerSubscriptionImpl(String type,
			RemoteObjectEventListener listener) {
		this.type = type;
		this.listener = listener;
	}

	public String getSubscription() {
		return subscriptionId;
	}

	public String getType() {
		return type;
	}

	public RemoteObjectEventListener getListener() {
		return listener;
	}

	@Override
	public String getSubscriptionId() {
		return subscriptionId;
	}

	public void setSubscription(String subscription) {
		this.subscriptionId = subscription;
	}
}
