package com.kurento.kmf.media.internal;

import com.kurento.kmf.media.ListenerRegistration;

public class ListenerRegistrationImpl implements ListenerRegistration {

	private final String callbackToken;

	public ListenerRegistrationImpl(String callbackToken) {
		this.callbackToken = callbackToken;
	}

	@Override
	public String getRegistrationId() {
		return callbackToken;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}

		if (obj == null) {
			return false;
		}

		if (getClass() != obj.getClass()) {
			return false;
		}

		ListenerRegistrationImpl other = (ListenerRegistrationImpl) obj;
		return callbackToken.equals(other.callbackToken);
	}

	@Override
	public int hashCode() {
		return callbackToken.hashCode();
	}

}
