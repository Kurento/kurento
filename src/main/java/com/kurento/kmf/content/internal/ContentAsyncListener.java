package com.kurento.kmf.content.internal;

import java.io.IOException;
import java.util.concurrent.Future;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.content.internal.jsonrpc.WebRtcJsonRequest;
import static com.kurento.kmf.content.internal.jsonrpc.WebRtcJsonConstants.*;
public class ContentAsyncListener implements AsyncListener {

	private static final Logger log = LoggerFactory
			.getLogger(ContentAsyncListener.class);

	static final String FUTURE_REQUEST_ATT_NAME = "kurento.future.request.att.name";

	static final String WEBRTC_MEDIA_REQUEST_ATT_NAME = "kurento.webrtcmedia.request.att.name";

	public static final String WEBRTC_JSON_REQUEST_ATT_NAME = "kurento.webrtcjsonrequest.request.att.name";

	// Public constructor is required by servlet spec
	public ContentAsyncListener() {
	}

	@Override
	public void onComplete(AsyncEvent ae) {
		HttpServletRequest request = (HttpServletRequest) ae.getAsyncContext()
				.getRequest();
		log.debug("AsyncListener: onComplete on request to: "
				+ request.getRequestURI());
	}

	@Override
	public void onTimeout(AsyncEvent ae) throws IOException {
		HttpServletRequest request = (HttpServletRequest) ae.getAsyncContext()
				.getRequest();

		log.debug("AsyncListener: onTimeout on request to: "
				+ request.getRequestURI());

		internalCompleteAsyncContext(
				ae,
				HttpServletResponse.SC_SERVICE_UNAVAILABLE,
				"Request processing timeout. You may tray to re-send your request later. Persistence of this error may be a symptom of a bug on application logic.");
	}

	private void internalCompleteAsyncContext(AsyncEvent ae, int status,
			String msg) throws IOException {
		AsyncContext asyncContext = ae.getAsyncContext();

		// If Handler is still executing, we try to cancel the task.
		Future<?> future = (Future<?>) asyncContext.getRequest().getAttribute(
				FUTURE_REQUEST_ATT_NAME);
		if (future != null) {
			future.cancel(true);
		}

		WebRtcMediaRequestImpl mediaRequest = (WebRtcMediaRequestImpl) asyncContext
				.getRequest().getAttribute(WEBRTC_MEDIA_REQUEST_ATT_NAME);
		WebRtcJsonRequest jsonRequest = (WebRtcJsonRequest)asyncContext
				.getRequest().getAttribute(WEBRTC_JSON_REQUEST_ATT_NAME);
		if (mediaRequest != null) {
			// This listener belongs to a WebRtcMediaService
			mediaRequest.terminate(asyncContext, ERROR_SERVER_ERROR , msg, jsonRequest.getId());
		} else {
			// This listener belongs to a player or recorder service
			((HttpServletResponse) asyncContext.getResponse()).sendError(
					status, msg);
			asyncContext.complete();
		}
	}

	@Override
	public void onError(AsyncEvent ae) throws IOException {
		HttpServletRequest request = (HttpServletRequest) ae.getAsyncContext()
				.getRequest();
		log.error(
				"AsyncListener: onError on request to: "
						+ request.getRequestURI(), ae.getThrowable());
		internalCompleteAsyncContext(ae,
				HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
				"Error processing request");
	}

	@Override
	public void onStartAsync(AsyncEvent ae) {
		HttpServletRequest request = (HttpServletRequest) ae.getAsyncContext()
				.getRequest();
		log.debug("AsyncListener: onStartAsync on request to: "
				+ request.getRequestURI());
	}

}
