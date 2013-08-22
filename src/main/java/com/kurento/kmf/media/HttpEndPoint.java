package com.kurento.kmf.media;

import java.io.IOException;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.protocol.TProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.media.HttpEndPointEvent.HttpEndPointEventType;
import com.kurento.kms.api.MediaEvent;
import com.kurento.kms.api.MediaObjectId;
import com.kurento.kms.api.MediaObjectNotFoundException;
import com.kurento.kms.api.MediaServerException;
import com.kurento.kms.api.MediaServerService;
import com.kurento.kms.api.MediaServerService.AsyncClient.getUrl_call;

/**
 * Represents a http address where a single get or post can be done
 * 
 * @author jcaden
 * 
 */
public class HttpEndPoint extends EndPoint {

	private static Logger log = LoggerFactory.getLogger(HttpEndPoint.class);

	private static final long serialVersionUID = 1L;

	HttpEndPoint(MediaObjectId httpEndPointId) {
		super(httpEndPointId);
	}

	public MediaEventListener<HttpEndPointEvent> addListener(
			MediaEventListener<HttpEndPointEvent> listener) {
		return handler.addListener(this, listener);
	}

	public boolean removeListener(MediaEventListener<HttpEndPointEvent> listener) {
		return handler.removeListener(this, listener);
	}

	@Override
	KmsEvent deserializeEvent(MediaEvent event) {
		try {
			TProtocol prot = handler.getProtocolFromEvent(event);

			com.kurento.kms.api.HttpEndPointEvent thriftEvent = new com.kurento.kms.api.HttpEndPointEvent();
			thriftEvent.read(prot);

			if (thriftEvent.isSetRequest()) {
				switch (thriftEvent.getRequest()) {
				case GET_REQUEST_EVENT:
					return new HttpEndPointEvent(this,
							HttpEndPointEventType.GET_REQUEST);
				case POST_REQUEST_EVENT:
					return new HttpEndPointEvent(this,
							HttpEndPointEventType.POST_REQUEST);
				case UNEXPECTED_REQUEST_EVENT:
					return new HttpEndPointEvent(this,
							HttpEndPointEventType.UNEXPECTED_REQUEST);
				}

			}

			log.error("Unexpected HttpEndPointEvent, falling back to default deserealizer");
		} catch (TException e) {
			log.error(
					"Error deserializing player event, falling back to default deserializer"
							+ e, e);
		}

		return super.deserializeEvent(event);
	}

	/* SYNC */

	public String getUrl() throws IOException {
		MediaServerService.Client service = mssm.getMediaServerService();

		try {
			return service.getUrl(mediaObjectId);
		} catch (MediaObjectNotFoundException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (MediaServerException e) {
			throw new RuntimeException(e.getMessage(), e);
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		} finally {
			mssm.releaseMediaServerService(service);
		}
	}

	/* ASYNC */

	public void getUrl(final Continuation<String> cont) throws IOException {
		MediaServerService.AsyncClient service = mssm
				.getMediaServerServiceAsync();

		try {
			service.getUrl(
					mediaObjectId,
					new AsyncMethodCallback<MediaServerService.AsyncClient.getUrl_call>() {
						@Override
						public void onComplete(getUrl_call response) {
							try {
								cont.onSuccess(response.getResult());
							} catch (MediaObjectNotFoundException e) {
								cont.onError(new RuntimeException(e
										.getMessage(), e));
							} catch (MediaServerException e) {
								cont.onError(new RuntimeException(e
										.getMessage(), e));
							} catch (TException e) {
								cont.onError(new IOException(e.getMessage(), e));
							}
						}

						@Override
						public void onError(Exception exception) {
							cont.onError(exception);
						}
					});
		} catch (TException e) {
			throw new IOException(e.getMessage(), e);
		} finally {
			mssm.releaseMediaServerServiceAsync(service);
		}
	}

}
