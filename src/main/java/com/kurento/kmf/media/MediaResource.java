package com.kurento.kmf.media;

public interface MediaResource {
		
	public void start();

	public void stop();

	/**
	 * Releases the resources associated to this Stream.
	 */
	public void release();

}
