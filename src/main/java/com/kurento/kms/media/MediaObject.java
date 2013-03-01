package com.kurento.kms.media;

import java.io.IOException;
import java.io.Serializable;

import org.apache.thrift.TException;

import com.kurento.kms.api.MediaObjectNotFoundException;
import com.kurento.kms.api.MediaServerException;
import com.kurento.kms.api.MediaServerService;
import com.kurento.kms.media.internal.MediaServerServiceManager;

public abstract class MediaObject implements Serializable {

	private static final long serialVersionUID = 1L;

	final com.kurento.kms.api.MediaObject mediaObject;

	MediaObject(com.kurento.kms.api.MediaObject mediaObject) {
		this.mediaObject = mediaObject;
	}

	public void release() throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.Client service = manager.getMediaServerService();
			service.release(mediaObject);
			manager.releaseMediaServerService(service);
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

}
