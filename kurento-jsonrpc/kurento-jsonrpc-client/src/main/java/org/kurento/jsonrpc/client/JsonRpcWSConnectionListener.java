package org.kurento.jsonrpc.client;

public interface JsonRpcWSConnectionListener {

	public void connectionTimeout();

	public void connected();

	public void disconnected();

}
