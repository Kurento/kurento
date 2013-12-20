package com.kurento.kmf.jsonrpcconnector;

import java.io.IOException;

import com.kurento.kmf.jsonrpcconnector.internal.JsonRpcRequestSender;

public interface Session extends JsonRpcRequestSender {

	public String getSessionId();

	public Object getRegisterInfo();

	public boolean isNew();

	public void close() throws IOException;

	void setReconnectionTimeout(long millis);

}
