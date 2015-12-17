
package org.kurento.client.internal;

@SuppressWarnings("serial")
public class NotEnoughResourcesException extends RuntimeException {

  public NotEnoughResourcesException(String message) {
    super(message);
  }

  public NotEnoughResourcesException(String message, Throwable cause) {
    super(message, cause);
  }

}
