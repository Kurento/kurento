package org.kurento.jsonrpc.client;

public interface JsonRpcWSConnectionListener {

	public void connected();

	public void connectionTimeout();

	public void disconnected();

}
