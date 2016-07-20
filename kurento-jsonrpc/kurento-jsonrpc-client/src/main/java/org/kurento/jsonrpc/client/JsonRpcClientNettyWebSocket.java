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
 */

package org.kurento.jsonrpc.client;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import javax.net.ssl.SSLException;

import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.websocket.api.UpgradeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;

public class JsonRpcClientNettyWebSocket extends AbstractJsonRpcClientWebSocket {

  public class JsonRpcWebSocketClientHandler extends AbstractJsonRpcWebSocketClientHandler {

    public JsonRpcWebSocketClientHandler(WebSocketClientHandshaker handshaker) {
      super(handshaker);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
      handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
      handshaker.handshake(ctx.channel());
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) {
      log.debug("Netty wesocket channel inactive");
      handleReconnectDisconnection(0, "Unknown reason");
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
      if (evt instanceof IdleStateEvent) {
        ctx.close();
      }
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
      Channel ch = ctx.channel();
      if (!handshaker.isHandshakeComplete()) {
        handshaker.finishHandshake(ch, (FullHttpResponse) msg);
        log.debug("WebSocket Client connected!");
        handshakeFuture.setSuccess();
        return;
      }

      if (msg instanceof FullHttpResponse) {
        FullHttpResponse response = (FullHttpResponse) msg;
        throw new IllegalStateException(
            "Unexpected FullHttpResponse (getStatus=" + response.status() + ", content="
                + response.content().toString(CharsetUtil.UTF_8) + ')');
      }

      WebSocketFrame frame = (WebSocketFrame) msg;
      if (frame instanceof TextWebSocketFrame) {
        TextWebSocketFrame textFrame = (TextWebSocketFrame) frame;
        receivedTextMessage(textFrame.text());
      } else {
        log.debug("Received frame of type {}. Will be ignored", frame.getClass().getSimpleName());
      }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      log.warn("Exception caught in Netty websocket handler", cause);
      if (!handshakeFuture.isDone()) {
        handshakeFuture.setFailure(cause);
      }
      ctx.close();
    }

  }

  private static final Logger log = LoggerFactory.getLogger(JsonRpcClientNettyWebSocket.class);

  private static final int MAX_PACKET_SIZE = 1000000;

  protected volatile Channel channel;
  protected volatile EventLoopGroup group;

  public JsonRpcClientNettyWebSocket(String url) {
    this(url, null, new SslContextFactory());
  }

  public JsonRpcClientNettyWebSocket(String url, SslContextFactory sslContextFactory) {
    this(url, null, sslContextFactory);
  }

  public JsonRpcClientNettyWebSocket(String url, JsonRpcWSConnectionListener connectionListener) {
    this(url, connectionListener, new SslContextFactory());
  }

  public JsonRpcClientNettyWebSocket(String url, JsonRpcWSConnectionListener connectionListener,
      SslContextFactory sslContextFactory) {
    super(url, connectionListener);
  }

  @Override
  protected void sendTextMessage(String jsonMessage) throws IOException {

    if (channel == null || !channel.isWritable() || !channel.isActive()) {
      throw new IllegalStateException(
          label + " JsonRpcClient is disconnected from WebSocket server at '" + this.uri + "'");
    }

    synchronized (channel) {
      channel.writeAndFlush(new TextWebSocketFrame(jsonMessage));
    }
  }

  @Override
  protected boolean isNativeClientConnected() {
    return channel != null && channel.isActive();
  }

  @Override
  protected void connectNativeClient() throws TimeoutException, Exception {

    if (channel == null || !channel.isActive() || group == null || group.isShuttingDown()
        || group.isShutdown()) {

      final boolean ssl = "wss".equalsIgnoreCase(this.uri.getScheme());
      final SslContext sslCtx;
      try {
        sslCtx = ssl ? SslContextBuilder.forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE).build() : null;
      } catch (SSLException e) {
        log.error("Could not create SSL Context", e);
        throw new IllegalArgumentException(
            "Could not create SSL context. See logs for more details", e);
      }

      group = new NioEventLoopGroup();

      // Connect with V13 (RFC 6455 aka HyBi-17). You can change it to V08 or V00.
      // If you change it to V00, ping is not supported and remember to change
      // HttpResponseDecoder to WebSocketHttpResponseDecoder in the pipeline.
      final JsonRpcWebSocketClientHandler handler =
          new JsonRpcWebSocketClientHandler(WebSocketClientHandshakerFactory.newHandshaker(uri,
              WebSocketVersion.V13, null, true, new DefaultHttpHeaders(), MAX_PACKET_SIZE));

      Bootstrap b = new Bootstrap();
      b.group(group).channel(NioSocketChannel.class)
          .handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
              ChannelPipeline p = ch.pipeline();
              p.addLast("idleStateHandler", new IdleStateHandler(0, 0, idleTimeout / 1000));
              if (sslCtx != null) {
                p.addLast(sslCtx.newHandler(ch.alloc(), uri.getHost(), uri.getPort()));
              }
              p.addLast(new HttpClientCodec(), new HttpObjectAggregator(8192),
                  WebSocketClientCompressionHandler.INSTANCE, handler);
            }
          }).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.connectionTimeout);

      int numRetries = 0;
      final int maxRetries = 5;
      while (channel == null || !channel.isOpen()) {
        try {
          channel = b.connect(uri.getHost(), uri.getPort()).sync().channel();
          handler.handshakeFuture().sync();
        } catch (InterruptedException e) {
          // This should never happen
          log.warn("ERROR connecting WS Netty client, opening channel", e);
        } catch (Exception e) {
          if (e.getCause() instanceof UpgradeException && numRetries < maxRetries) {
            log.warn(
                "Upgrade exception when trying to connect to {}. Try {} of {}. Retrying in 200ms ",
                uri, numRetries + 1, maxRetries);
            Thread.sleep(200);
            numRetries++;
          } else {
            throw e;
          }
        }

      }

    }

  }

  @Override
  public void closeNativeClient() {
    if (channel != null) {
      log.debug("{} Closing client", label);
      try {
        channel.close().sync();
      } catch (Exception e) {
        log.debug("{} Could not properly close websocket client. Reason: {}", label, e.getMessage(),
            e);
      }
      channel = null;
    } else {
      log.warn("{} Trying to close a JsonRpcClientNettyWebSocket with channel == null", label);
    }

    if (group != null) {
      group.shutdownGracefully();
    } else {
      log.warn("{} Trying to close a JsonRpcClientNettyWebSocket with group == null", label);
    }
    group = null;
  }

}
