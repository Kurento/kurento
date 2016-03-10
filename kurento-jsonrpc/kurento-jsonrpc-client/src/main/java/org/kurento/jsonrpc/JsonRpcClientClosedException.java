package org.kurento.jsonrpc;

/**
 * Exception thrown when trying to use a JsonRpcClient explicitly closed by user.
 * 
 * @author Micael Gallego (micael.gallego@gmail.com)
 *
 */
public class JsonRpcClientClosedException extends RuntimeException {

  private static final long serialVersionUID = -6830100526527848866L;

  public JsonRpcClientClosedException() {
  }

  public JsonRpcClientClosedException(String message) {
    super(message);
  }

  public JsonRpcClientClosedException(Throwable cause) {
    super(cause);
  }

  public JsonRpcClientClosedException(String message, Throwable cause) {
    super(message, cause);
  }

  public JsonRpcClientClosedException(String message, Throwable cause, boolean enableSuppression,
      boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }

}
