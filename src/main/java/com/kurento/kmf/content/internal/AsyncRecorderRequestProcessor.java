package com.kurento.kmf.content.internal;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.content.RecordRequest;
import com.kurento.kmf.content.RecorderHandler;

public class AsyncRecorderRequestProcessor implements RejectableRunnable {

	private static final Logger log = LoggerFactory
			.getLogger(AsyncRecorderRequestProcessor.class);

	private RecorderHandler recorderHandler;
	private RecordRequest recordRequest;

	public AsyncRecorderRequestProcessor(RecorderHandler recorderHandler,
			RecordRequest recordRequest) {
		this.recorderHandler = recorderHandler;
		this.recordRequest = recordRequest;
	}

	@Override
	public void run() {
		try {
			// In case this call ends without error, we do not complete the
			// AsyncContext. This makes possible recordRequest.play to be
			// asynchronous
			recorderHandler.onRecordRequest(recordRequest);
		} catch (Throwable t) {
			AsyncContext asyncCtx = recordRequest.getHttpServletRequest()
					.getAsyncContext();
			log.error(
					"Error processing onrecordRequest to "
							+ ((HttpServletRequest) asyncCtx.getRequest())
									.getRequestURI(), t);

			// In case of error we do complete the AsyncContext to make the
			// specific error visible for developers and final users.
			if (recordRequest.getHttpServletRequest().isAsyncStarted()) {
				try {
					((HttpServletResponse) asyncCtx.getResponse()).sendError(
							HttpServletResponse.SC_INTERNAL_SERVER_ERROR,
							"AsyncContext was not completed by application. Reason: "
									+ t.getMessage());
				} catch (IOException e) {
				}
				asyncCtx.complete();
			}
		}

	}

	@Override
	public void onExecutionRejected() {
		recordRequest.reject(HttpServletResponse.SC_SERVICE_UNAVAILABLE,
				"Servler overloaded. Try again in a few minutes");
	}
}
