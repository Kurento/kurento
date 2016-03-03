package org.kurento.jsonrpc.client;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public interface WebSocketClient {

  void sendTextMessage(String jsonMessage) throws IOException;

  void closeNativeClient();

  boolean isNativeClientConnected();

  void connectNativeClient() throws TimeoutException, Exception;

}
