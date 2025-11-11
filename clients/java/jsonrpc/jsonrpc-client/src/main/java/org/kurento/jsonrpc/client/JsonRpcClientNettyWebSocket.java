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
import java.nio.channels.spi.SelectorProvider;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.SSLException;

import org.kurento.commons.exception.KurentoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.DefaultSelectStrategyFactory;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultiThreadIoEventLoopGroup;
import io.netty.channel.nio.NioIoHandler;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.codec.http.websocketx.extensions.compression.WebSocketClientCompressionHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import io.netty.util.concurrent.RejectedExecutionHandlers;

public class JsonRpcClientNettyWebSocket extends AbstractJsonRpcClientWebSocket {

  public class JsonRpcWebSocketClientHandler extends AbstractJsonRpcWebSocketClientHandler {

    private StringBuilder partialText = new StringBuilder();

    public JsonRpcWebSocketClientHandler(WebSocketClientHandshaker handshaker) {
      super(handshaker);
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
      handshakeFuture = ctx.newPromise();
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {
      log.debug("{} channel active", label);
      handshaker.handshake(ctx.channel());
    }

    @Override
    public void channelWritabilityChanged(ChannelHandlerContext ctx) {
      log.debug("{} channel inactive", label);
      handleReconnectDisconnection(0, "Unknown reason");
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
      if (evt instanceof IdleStateEvent) {
        log.debug("{} Idle state event received", label);
        handleReconnectDisconnection(0, "Idle event received");
      }
    }

    @Override
    public void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
      Channel ch = ctx.channel();
      if (!handshaker.isHandshakeComplete()) {
        handshaker.finishHandshake(ch, (FullHttpResponse) msg);
        log.debug("{} WebSocket Client connected!", label);
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
        if (textFrame.isFinalFragment()) {
          receivedTextMessage(textFrame.text());
        } else {
          partialText.append(textFrame.text());
        }
      } else if (frame instanceof ContinuationWebSocketFrame) {
        ContinuationWebSocketFrame continuationFrame = (ContinuationWebSocketFrame) frame;
        partialText.append(continuationFrame.text());
        if (continuationFrame.isFinalFragment()) {
          receivedTextMessage(partialText.toString());
          partialText.setLength(0);
        }
      } else if (frame instanceof CloseWebSocketFrame) {
        CloseWebSocketFrame closeFrame = (CloseWebSocketFrame) frame;
        log.info("{} Received close frame from server. Will close client! Reason: {}", label,
            closeFrame.reasonText());
      } else {
        log.warn("{} Received frame of type {}. Will be ignored", label,
            frame.getClass().getSimpleName());
      }

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
      log.warn("{} Exception caught in Netty websocket handler", label, cause);
      if (!handshakeFuture.isDone()) {
        handshakeFuture.setFailure(cause);
      }
      try {
        close();
      } catch (IOException e) {
        log.warn("{} Exception closing Netty websocket client", label);
      }
    }

  }

  private static final Logger log = LoggerFactory.getLogger(JsonRpcClientNettyWebSocket.class);

  private final Lock lock = new ReentrantLock();
  private final Condition condition = lock.newCondition();
  
  private volatile Channel channel;
  private volatile EventLoopGroup group;
  private volatile JsonRpcWebSocketClientHandler handler;

  public JsonRpcClientNettyWebSocket(String url) {
    this(url, null);
  }

  public JsonRpcClientNettyWebSocket(String url, JsonRpcWSConnectionListener connectionListener) {
    super(url, connectionListener);
    log.debug("{} Creating JsonRPC NETTY Websocket client", label);
  }

  public void waitForChannelWritability() throws InterruptedException, KurentoException {
    lock.lock();
    try {
      // 1 second is way too much, but we need to be sure that the channel is writable
      if (!condition.await(1, TimeUnit.SECONDS)) {
        // If the channel is not writable after 1 second, we throw an exception
        if (!channel.isWritable()) {
          log.warn("{} channel is not writable, request is discarded", label);
          throw new KurentoException("label channel is not writable, request is discarded");
        }
      }
    } finally {
        lock.unlock();
    }
  }  

  @Override
  protected void sendTextMessage(String jsonMessage) throws IOException {
    boolean delivered = false;

    if (channel == null || !channel.isActive()) {
      throw new IllegalStateException(
          label + " JsonRpcClient is disconnected from WebSocket server at '" + this.uri + "'");
    }

    while (! delivered) {
      boolean retry = false;

      synchronized (channel) {
        if (channel.isWritable()) {
          channel.writeAndFlush(new TextWebSocketFrame(jsonMessage));
          delivered = true;
        } else {
          log.warn("{} channel is not writable, request will be enqueued", label);
          retry = true;
        }
      }
      if (retry) {
        // Backpressure: wait for channel to be writable
        // We wait for at most 1 second and if not writable an exception is thrown
        try {
          waitForChannelWritability();
        } catch (InterruptedException e) {
          log.warn("{} Interrupted while waiting for channel writability", label);
          throw new IOException("Interrupted while waiting for channel writability", e);
        }
      }
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

      log.info("{} Connecting native client", label);

      final boolean ssl = "wss".equalsIgnoreCase(this.uri.getScheme());
      final SslContext sslCtx;
      try {
        sslCtx = ssl ? SslContextBuilder.forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE).build() : null;
      } catch (SSLException e) {
        log.error("{} Could not create SSL Context", label, e);
        throw new IllegalArgumentException(
            "Could not create SSL context. See logs for more details", e);
      }

      final String scheme = uri.getScheme() == null ? "ws" : uri.getScheme();
      final String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
      final int port;
      if (uri.getPort() == -1) {
        if ("ws".equalsIgnoreCase(scheme)) {
          port = 80;
        } else if ("wss".equalsIgnoreCase(scheme)) {
          port = 443;
        } else {
          port = -1;
        }
      } else {
        port = uri.getPort();
      }

      if (group == null || group.isShuttingDown() || group.isShutdown() || group.isTerminated()) {
        log.info("{} Creating new native event loop", label);
        group = new MultiThreadIoEventLoopGroup(0, (Executor) null,
            NioIoHandler.newFactory(SelectorProvider.provider(), DefaultSelectStrategyFactory.INSTANCE));
      }

      if (channel != null) {
        log.info("{} Closing previously existing channel when connecting native client", label);
        closeChannel();
      }

      Bootstrap b = new Bootstrap();
      b.group(group).channel(NioSocketChannel.class)
          .handler(new ChannelInitializer<SocketChannel>() {
            @Override
            protected void initChannel(SocketChannel ch) {
              log.info("{} Initiating new Netty channel. Will create new handler too!", label);
              handler = new JsonRpcWebSocketClientHandler(
                  WebSocketClientHandshakerFactory.newHandshaker(uri, WebSocketVersion.V13, null,
                      true, new DefaultHttpHeaders(), maxPacketSize));

              ChannelPipeline p = ch.pipeline();
              p.addLast("idleStateHandler", new IdleStateHandler(0, 0, idleTimeout / 1000));
              if (sslCtx != null) {
                p.addLast(sslCtx.newHandler(ch.alloc(), host, port));
              }
              p.addLast(new HttpClientCodec(), new HttpObjectAggregator(8192),
                  new WebSocketClientCompressionHandler(0), handler);
            }
          }).option(ChannelOption.CONNECT_TIMEOUT_MILLIS, this.connectionTimeout);

      int numRetries = 0;
      final int maxRetries = 5;
      while (channel == null || !channel.isOpen()) {
        try {
          channel = b.connect(host, port).sync().channel();
          handler.handshakeFuture().sync();
        } catch (InterruptedException e) {
          // This should never happen
          log.warn("{} ERROR connecting WS Netty client, opening channel", label, e);
        } catch (Exception e) {
          if (e.getCause() instanceof WebSocketHandshakeException && numRetries < maxRetries) {
            log.warn(
                "{} Upgrade exception when trying to connect to {}. Try {} of {}. Retrying in 200ms ",
                label, uri, numRetries + 1, maxRetries);
            Thread.sleep(200);
            numRetries++;
          } else {
            throw e;
          }
        }

      }

      channel.closeFuture().addListener(new ChannelFutureListener() {
        @Override
        public void operationComplete(ChannelFuture future) throws Exception {
          log.info("{} channel closed", label);
          handleReconnectDisconnection(1001, "Channel closed");
        }
      });

      channel.pipeline().addLast(new ChannelInboundHandlerAdapter() {
        @Override
        public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
          boolean writable = ctx.channel().isWritable();
          log.info("{} channel writability changed {}", label, writable);
          if (writable) {
            lock.lock();
            try {
              condition.signalAll();
            } finally {
              lock.unlock();
            }
          }
      }
    });

    }

  }

  @Override
  public void closeNativeClient() {
    closeChannel();

    if (group != null) {
      group.shutdownGracefully();
    } else {
      log.warn("{} Trying to close a JsonRpcClientNettyWebSocket with group == null", label);
    }
    group = null;
    handler = null;
  }

  private Future<Void> closeChannel() {
    if (channel != null) {
      log.debug("{} Closing client", label);
      try {
        return channel.close();
      } catch (Exception e) {
        log.debug("{} Could not properly close websocket client. Reason: {}", label, e.getMessage(),
            e);
      }
      channel = null;
    } else {
      log.warn("{} Trying to close a JsonRpcClientNettyWebSocket with channel == null", label);
    }
    return null;
  }

}
