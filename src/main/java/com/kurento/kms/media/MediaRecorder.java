package com.kurento.kms.media;

import java.io.IOException;

import org.apache.thrift.TException;

import com.kurento.kms.api.MediaObjectNotFoundException;
import com.kurento.kms.api.MediaServerException;
import com.kurento.kms.api.MediaServerService;
import com.kurento.kms.media.internal.MediaServerServiceManager;

public class MediaRecorder extends Joinable {

	private static final long serialVersionUID = 1L;

	public MediaRecorder(com.kurento.kms.api.MediaObject mediaRecorder) {
		super(mediaRecorder);
	}

	void record() throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.Client service = manager.getMediaServerService();
			service.record(mediaObject);
			manager.releaseMediaServerService(service);
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	void pause() throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.Client service = manager.getMediaServerService();
			service.pauseRecorder(mediaObject);
			manager.releaseMediaServerService(service);
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	void stop() throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.Client service = manager.getMediaServerService();
			service.stopRecorder(mediaObject);
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
