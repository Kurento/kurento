
package org.kurento.client;

public class KurentoClientBuilder {

  private Properties properties;
  private String kmsWsUri;

  private Handler connectedHandler;
  private Handler connectionFailedHandler;
  private Handler disconnectedHandler;
  private ReconnectedHandler reconnectedHandler;
  private Handler reconnectingHandler;

  private Long tryReconnectingMaxTime;
  private Long connectionTimeout;

  public KurentoClientBuilder() {
  }

  public KurentoClientBuilder setProperties(Properties properties) {
    this.properties = properties;
    return this;
  }

  public KurentoClientBuilder setKmsWsUri(String kmsWsUri) {
    this.kmsWsUri = kmsWsUri;
    return this;
  }

  public KurentoClientBuilder onConnected(Handler connectedHandler) {
    this.connectedHandler = connectedHandler;
    return this;
  }

  public KurentoClientBuilder onConnectionFailed(Handler connectionFailedHandler) {
    this.connectionFailedHandler = connectionFailedHandler;
    return this;
  }

  public KurentoClientBuilder onDisconnected(Handler disconnectedHandler) {
    this.disconnectedHandler = disconnectedHandler;
    return this;
  }

  public KurentoClientBuilder onReconnecting(Handler reconnectingHandler) {
    this.reconnectingHandler = reconnectingHandler;
    return this;
  }

  public KurentoClientBuilder onReconnected(ReconnectedHandler reconnectedHandler) {
    this.reconnectedHandler = reconnectedHandler;
    return this;
  }

  public KurentoClientBuilder setTryReconnectingMaxTime(Long tryReconnectingMaxTime) {
    this.tryReconnectingMaxTime = tryReconnectingMaxTime;
    return this;
  }

  public KurentoClientBuilder setConnectionTimeout(Long connectionTimeout) {
    this.connectionTimeout = connectionTimeout;
    return this;
  }

  public KurentoClient connect() {
    return KurentoClient.create(kmsWsUri, properties, connectedHandler, connectionFailedHandler,
        reconnectingHandler, disconnectedHandler, reconnectedHandler, tryReconnectingMaxTime,
        connectionTimeout);
  }

}
