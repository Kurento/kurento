package com.kurento.kms.media;

import java.io.IOException;

import org.apache.thrift.TException;

import com.kurento.kms.api.MediaObjectNotFoundException;
import com.kurento.kms.api.MediaServerException;
import com.kurento.kms.api.MediaServerService;
import com.kurento.kms.media.internal.MediaServerServiceManager;

public class Mixer extends MediaObject {

	private static final long serialVersionUID = 1L;

	public Mixer(com.kurento.kms.api.MediaObject mixer) {
		super(mixer);
	}

	MixerPort getPort() throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.Client service = manager.getMediaServerService();
			com.kurento.kms.api.MediaObject mixerPort = service
					.getMixerPort(mediaObject);
			manager.releaseMediaServerService(service);
			return new MixerPort(mixerPort);
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

}
