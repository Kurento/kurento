package com.kurento.kms.media;

import java.io.IOException;
import java.io.Serializable;
import java.util.Properties;

import org.apache.thrift.TException;

import com.kurento.kms.api.MediaObjectNotFoundException;
import com.kurento.kms.api.MediaServerException;
import com.kurento.kms.api.MediaServerService;
import com.kurento.kms.media.internal.KmsConstants;
import com.kurento.kms.media.internal.MediaServerServiceManager;

public class MediaFactory implements Serializable {

	private static final long serialVersionUID = 1L;

	private String mediaServerAddress;
	private int mediaServerPort;

	private com.kurento.kms.api.MediaObject mediaFactory;

	public MediaFactory(Properties properties) throws MediaException {
		processProperties(properties);

		try {
			// TODO: Make this clusterizable to can use some media servers.
			MediaServerServiceManager.init(mediaServerAddress, mediaServerPort);
		} catch (TException e) {
			throw new MediaException(e.getMessage(), e);
		}

		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.Client service = manager.getMediaServerService();
			com.kurento.kms.api.MediaObject stream = service
					.createMediaFactory();
			mediaFactory = service.createMediaFactory();
			manager.releaseMediaServerService(service);
		} catch (MediaServerException e) {
			throw new MediaException(e.getMessage(), e);
		} catch (TException e) {
			throw new MediaException(e.getMessage(), e);
		}
	}

	public MediaPlayer getMediaPlayer(String uri) throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.Client service = manager.getMediaServerService();
			com.kurento.kms.api.MediaObject player = service
					.createMediaPlayer(mediaFactory);
			manager.releaseMediaServerService(service);
			return new MediaPlayer(player);
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	public MediaRecorder getMediaRecorder(String uri) throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.Client service = manager.getMediaServerService();
			com.kurento.kms.api.MediaObject recorder = service
					.createMediaRecorder(mediaFactory);
			manager.releaseMediaServerService(service);
			return new MediaRecorder(recorder);
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	public Stream getStream() throws MediaException, IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.Client service = manager.getMediaServerService();
			com.kurento.kms.api.MediaObject stream = service
					.createStream(mediaFactory);
			manager.releaseMediaServerService(service);
			return new Stream(stream);
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	public <T extends Mixer> T getMixer(Class<T> clazz) {
		return null;
	}

	private void processProperties(Properties properties) {
		if (properties.getProperty(KmsConstants.SERVER_ADDRESS) == null) {
			mediaServerAddress = KmsConstants.DEFAULT_SERVER_ADDRESS;
		} else {
			mediaServerAddress = properties
					.getProperty(KmsConstants.SERVER_ADDRESS);
		}

		if (properties.getProperty(KmsConstants.SERVER_PORT) == null) {
			mediaServerPort = KmsConstants.DEFAULT_SERVER_PORT;
		} else {
			try {
				mediaServerPort = Integer.parseInt(properties
						.getProperty(KmsConstants.SERVER_PORT));
			} catch (NumberFormatException e) {
				mediaServerPort = KmsConstants.DEFAULT_SERVER_PORT;
			}
		}
	}

}
