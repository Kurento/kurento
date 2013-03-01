package com.kurento.kms.media;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.thrift.TException;

import com.kurento.kms.api.JoinException;
import com.kurento.kms.api.MediaObjectNotFoundException;
import com.kurento.kms.api.MediaServerException;
import com.kurento.kms.api.MediaServerService;
import com.kurento.kms.api.MediaType;
import com.kurento.kms.media.internal.MediaServerServiceManager;

public abstract class Joinable extends MediaObject {

	private static final long serialVersionUID = 1L;

	public Joinable(com.kurento.kms.api.MediaObject joinable) {
		super(joinable);
	}

	public void join(Joinable peer) throws MediaException, IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.Client service = manager.getMediaServerService();
			service.join(mediaObject, peer.mediaObject);
			manager.releaseMediaServerService(service);
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (JoinException e) {
			throw new MediaException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	public void unjoin(Joinable peer) throws MediaException, IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.Client service = manager.getMediaServerService();
			service.unjoin(mediaObject, peer.mediaObject);
			manager.releaseMediaServerService(service);
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (JoinException e) {
			throw new MediaException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	public Collection<MediaSrc> getMediaSrcs() throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.Client service = manager.getMediaServerService();
			List<com.kurento.kms.api.MediaObject> tMediaSrcs = service
					.getMediaSrcs(mediaObject);
			List<MediaSrc> mediaSrcs = new ArrayList<MediaSrc>();
			for (com.kurento.kms.api.MediaObject tms : tMediaSrcs) {
				mediaSrcs.add(new MediaSrc(tms));
			}
			manager.releaseMediaServerService(service);
			return mediaSrcs;
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	public Collection<MediaSink> getMediaSinks() throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.Client service = manager.getMediaServerService();
			List<com.kurento.kms.api.MediaObject> tMediaSinks = service
					.getMediaSinks(mediaObject);
			List<MediaSink> mediaSinks = new ArrayList<MediaSink>();
			for (com.kurento.kms.api.MediaObject tms : tMediaSinks) {
				mediaSinks.add(new MediaSink(tms));
			}
			manager.releaseMediaServerService(service);
			return mediaSinks;
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	public Collection<MediaSrc> getMediaSrcs(MediaType mediaType)
			throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.Client service = manager.getMediaServerService();
			List<com.kurento.kms.api.MediaObject> tMediaSrcs = service
					.getMediaSrcsByMediaType(mediaObject, mediaType);
			List<MediaSrc> mediaSrcs = new ArrayList<MediaSrc>();
			for (com.kurento.kms.api.MediaObject tms : tMediaSrcs) {
				mediaSrcs.add(new MediaSrc(tms));
			}
			manager.releaseMediaServerService(service);
			return mediaSrcs;
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

	public Collection<MediaSink> getMediaSinks(MediaType mediaType)
			throws IOException {
		try {
			MediaServerServiceManager manager = MediaServerServiceManager
					.getInstance();
			MediaServerService.Client service = manager.getMediaServerService();
			List<com.kurento.kms.api.MediaObject> tMediaSinks = service
					.getMediaSinksByMediaType(mediaObject, mediaType);
			List<MediaSink> mediaSinks = new ArrayList<MediaSink>();
			for (com.kurento.kms.api.MediaObject tms : tMediaSinks) {
				mediaSinks.add(new MediaSink(tms));
			}
			manager.releaseMediaServerService(service);
			return mediaSinks;
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		}
	}

}
