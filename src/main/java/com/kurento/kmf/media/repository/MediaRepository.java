package com.kurento.kmf.media.repository;

import java.io.IOException;

public abstract class MediaRepository {

	/**
	 * 
	 * @return the created media id
	 * @throws IOException
	 *             if the media resource cannot be created
	 */
	public abstract String create() throws IOException;

	/**
	 * 
	 * @param mediaId
	 * @throws IOException
	 *             it the media resource cannot be removed
	 */
	public abstract void remove(String mediaId) throws IOException;

	/**
	 * 
	 * @param mediaId
	 * @return the URI of the media resource with this id
	 * @throws IOException
	 *             if the media resource does not exist
	 */
	public abstract String getUri(String mediaId) throws IOException;

}
