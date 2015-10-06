package org.kurento.client.internal;

public interface KmsUrlProvider {

	public String getKmsUrl(int loadPoints) throws NotEnoughResourcesException;

	public String getKmsUrl() throws NotEnoughResourcesException;

}
