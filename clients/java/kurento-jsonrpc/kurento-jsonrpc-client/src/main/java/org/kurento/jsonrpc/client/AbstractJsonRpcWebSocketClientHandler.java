
package org.kurento.jsonrpc.client;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelPromise;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker;

public abstract class AbstractJsonRpcWebSocketClientHandler
    extends SimpleChannelInboundHandler<Object> {

  protected final WebSocketClientHandshaker handshaker;
  protected ChannelPromise handshakeFuture;

  public AbstractJsonRpcWebSocketClientHandler(WebSocketClientHandshaker handshaker) {
    this.handshaker = handshaker;
  }

  public ChannelFuture handshakeFuture() {
    return handshakeFuture;
  }

}
