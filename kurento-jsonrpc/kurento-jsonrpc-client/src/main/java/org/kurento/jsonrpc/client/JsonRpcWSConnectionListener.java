package org.kurento.jsonrpc.client;

public interface JsonRpcWSConnectionListener {

	/**
	 * Method invoked when the JsonRpcWS client successfully connects to the
	 * server
	 */
	void connected();

	/**
	 * Method invoked when the JsonRpcWS client could not connect to the server.
	 * This method can be invoked also if a reconnection is needed.
	 */
	void connectionFailed();

	/**
	 * Method invoked when the JsonRpcWS client connection with the server is
	 * interrupted
	 */
	void disconnected();

	/**
	 * Method invoked when the JsonRpcWS client is reconnected to a server
	 */
	void reconnected(boolean sameServer);

}
