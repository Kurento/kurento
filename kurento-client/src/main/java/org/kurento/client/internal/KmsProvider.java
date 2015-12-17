
package org.kurento.client.internal;

public interface KmsProvider {

  public String reserveKms(String id, int loadPoints) throws NotEnoughResourcesException;

  public String reserveKms(String id) throws NotEnoughResourcesException;

  public void releaseKms(String id) throws NotEnoughResourcesException;

}
