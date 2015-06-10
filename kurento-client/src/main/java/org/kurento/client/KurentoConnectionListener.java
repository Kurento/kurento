package org.kurento.client;

public interface KurentoConnectionListener {

	/**
	 * Method invoked when the Kurento client successfully connects to the
	 * server
	 */
	void connected();

	/**
	 * Method invoked when the Kurento client could not connect to the server.
	 * This method can be invoked also if a reconnection is needed.
	 */
	void connectionFailed();

	/**
	 * Method invoked when the Kurento client connection with the server is
	 * interrupted
	 */
	void disconnected();

}
