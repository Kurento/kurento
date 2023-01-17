package org.kurento.jsonrpc.client;

public interface ReconnectedHandler {
  public void run(boolean sameServer);
}