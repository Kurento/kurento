/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.kurento.client;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

import javax.annotation.PreDestroy;

import org.kurento.client.internal.KmsUrlLoader;
import org.kurento.client.internal.TransactionImpl;
import org.kurento.client.internal.client.RomManager;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient;
import org.kurento.commons.PropertiesManager;
import org.kurento.commons.exception.KurentoException;
import org.kurento.jsonrpc.client.AbstractJsonRpcClientWebSocket;
import org.kurento.jsonrpc.client.JsonRpcClient;
import org.kurento.jsonrpc.client.JsonRpcClientNettyWebSocket;
import org.kurento.jsonrpc.message.Request;
import org.kurento.jsonrpc.message.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.StandardSystemProperty;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * Factory to create {@link MediaPipeline} in the media server.
 *
 * @author Luis López (llopez@gsyc.es)
 * @author Ivan Gracia (igracia@gsyc.es)
 * @author Micael Gallego (micael.gallego@gmail.com)
 * @since 2.0.0
 */
public class KurentoClient {

  private static final int KEEPALIVE_TIME =
	      PropertiesManager.getProperty("kurento.client.keepAliveTime", 240000);

  private static final long WARN_CONNECTION_TIME = 5000;

  private static Logger log = LoggerFactory.getLogger(KurentoClient.class);

  protected RomManager manager;

  private long requesTimeout =
      PropertiesManager.getProperty("kurento.client.requestTimeout", 10000);

  private long connectionTimeout =
      PropertiesManager.getProperty("kurento.client.connectionTimeout", 5000);

  private String id;

  private ServerManager serverManager;

  private JsonRpcClient client;

  private static KmsUrlLoader kmsUrlLoader;

  private String label;
  
  private boolean destroyed = false;

  public static synchronized String getKmsUrl(String id, Properties properties) {

    if (properties == null) {
      properties = new Properties();
    }

    if (kmsUrlLoader == null) {

      Path configFile =
          Paths.get(StandardSystemProperty.USER_HOME.value(), ".kurento", "config.properties");

      kmsUrlLoader = new KmsUrlLoader(configFile);
    }

    Object load = properties.get("loadPoints");
    if (load == null) {
      return kmsUrlLoader.getKmsUrl(id);
    } else {
      if (load instanceof Number) {
        return kmsUrlLoader.getKmsUrlLoad(id, ((Number) load).intValue());
      } else {
        return kmsUrlLoader.getKmsUrlLoad(id, Integer.parseInt(load.toString()));
      }
    }
  }

  private void setId(String id) {
    this.id = id;
  }

  public static KurentoClient create() {
    return create(new Properties());
  }

  public static KurentoClient create(Properties properties) {
    String id = UUID.randomUUID().toString();
    KurentoClient client = create(getKmsUrl(id, properties), properties);
    client.setId(id);
    return client;
  }

  public static KurentoClient create(String websocketUrl) {
    return create(websocketUrl, new Properties());
  }

  public static KurentoClient create(String websocketUrl, Properties properties) {
    log.debug("Connecting to kms in {}", websocketUrl);
    JsonRpcClientNettyWebSocket client = new JsonRpcClientNettyWebSocket(websocketUrl);
    configureJsonRpcClient(client);
    return new KurentoClient(client);
  }

  protected static void configureJsonRpcClient(AbstractJsonRpcClientWebSocket client) {
    client.enableHeartbeat(KEEPALIVE_TIME);
    client.setTryReconnectingForever(true);
    updateLabel(client, null);
    client.setSendCloseMessage(true);
  }

  public static KurentoClient create(String websocketUrl, KurentoConnectionListener listener) {
    return create(websocketUrl, listener, new Properties());
  }

  public static KurentoClient create(Properties properties, KurentoConnectionListener listener) {
    String id = UUID.randomUUID().toString();
    KurentoClient client = create(getKmsUrl(id, properties), listener, properties);
    client.setId(id);
    return client;
  }

  public static KurentoClient create(String websocketUrl, KurentoConnectionListener listener,
      Properties properties) {
    log.debug("Connecting to KMS in {}", websocketUrl);
    JsonRpcClientNettyWebSocket client = new JsonRpcClientNettyWebSocket(websocketUrl,
        JsonRpcConnectionListenerKurento.create(listener));
    configureJsonRpcClient(client);
    return new KurentoClient(client);
  }

  protected static KurentoClient create(String kmsWsUri, Properties properties,
      final Handler connectedHandler, final Handler connectionFailedHandler,
      final Handler reconnectingHandler, final Handler disconnectedHandler,
      final ReconnectedHandler reconnectedHandler, Long tryReconnectingMaxTime,
      Long connectionTimeout) {

    String clientId = null;
    if (kmsWsUri == null) {
      clientId = UUID.randomUUID().toString();
      kmsWsUri = getKmsUrl(clientId, properties);
    }

    KurentoClient kurentoClient = null;

    log.debug("Connecting to KMS in {}", kmsWsUri);

    JsonRpcClientNettyWebSocket client = new JsonRpcClientNettyWebSocket(kmsWsUri);

    if (connectionTimeout != null) {
      client.setConnectionTimeout(connectionTimeout.intValue());
    }

    if (connectedHandler != null) {
      client.onConnected(new org.kurento.jsonrpc.client.Handler() {
        @Override
        public void run() {
          connectedHandler.run();
        }
      });
    }

    if (connectionFailedHandler != null) {
      client.onConnectionFailed(new org.kurento.jsonrpc.client.Handler() {
        @Override
        public void run() {
          connectionFailedHandler.run();
        }
      });
    }

    if (reconnectingHandler != null) {
      client.onReconnecting(new org.kurento.jsonrpc.client.Handler() {
        @Override
        public void run() {
          reconnectingHandler.run();
        }
      });
    }

    if (disconnectedHandler != null) {
      client.onDisconnected(new org.kurento.jsonrpc.client.Handler() {
        @Override
        public void run() {
          disconnectedHandler.run();
        }
      });
    }

    if (reconnectedHandler != null) {
      client.onReconnected(new org.kurento.jsonrpc.client.ReconnectedHandler() {
        @Override
        public void run(boolean sameServer) {
          reconnectedHandler.run(sameServer);
        }
      });
    }

    configureJsonRpcClient(client);

    if (tryReconnectingMaxTime != null) {
      client.setTryReconnectingMaxTime(tryReconnectingMaxTime);
    }

    kurentoClient = new KurentoClient(client);

    if (clientId != null) {
      kurentoClient.setId(clientId);
    }

    return kurentoClient;
  }

  protected KurentoClient(JsonRpcClient client) {
	  this(client, true);
  }

  protected KurentoClient(JsonRpcClient client, boolean overrideClientTimeouts) {
    this.client = client;
    this.manager = new RomManager(new RomClientJsonRpcClient(client));
    if (overrideClientTimeouts) {
      client.setRequestTimeout(requesTimeout);
      client.setConnectionTimeout((int) connectionTimeout);
    }
    if (client instanceof AbstractJsonRpcClientWebSocket) {
      ((AbstractJsonRpcClientWebSocket) client).enableHeartbeat(KEEPALIVE_TIME);
    }
    try {
      long start = System.currentTimeMillis();
      client.connect();
      long duration = System.currentTimeMillis() - start;

      if (duration > WARN_CONNECTION_TIME) {
        log.warn("Connected to KMS in {} millis (> {} millis)", duration, WARN_CONNECTION_TIME);
      }

    } catch (Exception e) {
      throw new KurentoException("Exception connecting to KMS", e);
    }
  }

  /**
   * Creates a new {@link MediaPipeline} in the media server.
   *
   * @return The media pipeline
   */
  public MediaPipeline createMediaPipeline() {
    return new AbstractBuilder<MediaPipeline>(MediaPipeline.class, manager).build();
  }

  /**
   * Creates a new {@link MediaPipeline} in the media server.
   *
   * @param cont
   *          An asynchronous callback handler. If the element was successfully created, the
   *          {@code onSuccess} method from the handler will receive a {@link MediaPipeline} stub
   *          from the media server.
   * @throws KurentoException
   *
   */
  public void createMediaPipeline(final Continuation<MediaPipeline> cont) throws KurentoException {
    new AbstractBuilder<MediaPipeline>(MediaPipeline.class, manager).buildAsync(cont);
  }

  public MediaPipeline createMediaPipeline(Transaction tx) {
    return new AbstractBuilder<MediaPipeline>(MediaPipeline.class, manager).build(tx);
  }

  /**
   * Creates a new {@link MediaPipeline} in the media server.
   *
   * @return The media pipeline
   */
  public MediaPipeline createMediaPipeline(Properties properties) {
    return new AbstractBuilder<MediaPipeline>(MediaPipeline.class, manager)
        .withProperties(properties).build();
  }

  /**
   * Creates a new {@link MediaPipeline} in the media server.
   *
   * @param cont
   *          An asynchronous callback handler. If the element was successfully created, the
   *          {@code onSuccess} method from the handler will receive a {@link MediaPipeline} stub
   *          from the media server.
   * @throws KurentoException
   *
   */
  public void createMediaPipeline(Properties properties, final Continuation<MediaPipeline> cont)
      throws KurentoException {
    new AbstractBuilder<MediaPipeline>(MediaPipeline.class, manager).withProperties(properties)
        .buildAsync(cont);
  }

  public MediaPipeline createMediaPipeline(Transaction tx, Properties properties) {
    return new AbstractBuilder<MediaPipeline>(MediaPipeline.class, manager)
        .withProperties(properties).build(tx);
  }

  @PreDestroy
  public void destroy() {
	this.destroyed = true;
    if (isClosed()) {
      log.debug("{} KurentoClient already closed", label);
      return;
    }
    log.debug("Closing KurentoClient");
    manager.destroy();
    try {
      client.close();
    } catch (IOException e) {
      log.error("Error while closing KurentoClient", e);
    }
    if (kmsUrlLoader != null) {
      kmsUrlLoader.clientDestroyed(id);
    }
  }

  public boolean isClosed() {
    return manager.getRomClient().isClosed();
  }

  public boolean isDestroyed() {
    return this.destroyed;
  }

  public static KurentoClient createFromJsonRpcClient(JsonRpcClient jsonRpcClient) {
    return new KurentoClient(jsonRpcClient);
  }

  public static KurentoClient createFromJsonRpcClientHonoringClientTimeouts(JsonRpcClient jsonRpcClient) {
    return new KurentoClient(jsonRpcClient, false);
  }

  public Transaction beginTransaction() {
    return new TransactionImpl(manager);
  }

  public ServerManager getServerManager() {
    if (serverManager == null) {
      serverManager = getById("manager_ServerManager", ServerManager.class);
    }
    return serverManager;
  }

  public <T extends KurentoObject> T getById(String id, Class<T> clazz) {
    return manager.getById(id, clazz);
  }

  public String getSessionId() {
    return client.getSession().getSessionId();
  }

  public void setLabel(String label) {
    this.label = label;
    updateLabel(client, label);
  }

  public String getLabel() {
    return label;
  }

  private static void updateLabel(JsonRpcClient client, String label) {
    String clientLabel = "KurentoClient";
    if (label != null) {
      clientLabel += ":" + label;
    }
    client.setLabel(clientLabel);
  }

  public Response<JsonElement> sendJsonRpcRequest(Request<JsonObject> request) throws IOException {
	  return client.sendRequest(request);
  }
}
