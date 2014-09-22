package org.kurento.client;

public interface KurentoConnectionListener {

	public void connectionTimeout();

	public void connected();

	public void disconnected();

}
