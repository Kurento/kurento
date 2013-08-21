package com.kurento.kmf.content.internal.base;

import java.io.IOException;

import javax.servlet.AsyncContext;

import org.springframework.util.Assert;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.internal.ContentRequestManager;
import com.kurento.kmf.content.internal.jsonrpc.JsonRpcResponse;
import com.kurento.kmf.media.MediaElement;

public abstract class AbstractSdpBasedMediaRequest extends
		AbstractContentRequest {

	public AbstractSdpBasedMediaRequest(ContentRequestManager manager,
			AsyncContext asyncContext, String contentId) {
		super(manager, asyncContext, contentId);
	}

	protected abstract String buildMediaEndPointAndReturnSdp(
			MediaElement upStream, MediaElement downStream);

	public void startMedia(MediaElement upStream, MediaElement downStream)
			throws ContentException {
		synchronized (this) {
			Assert.isTrue(state == STATE.HANDLING,
					"Cannot start media exchange in state " + state
							+ ". This error means ..."); // TODO further
															// explanation
			state = STATE.STARTING;
		}

		getLogger().debug("SDP received " + initialJsonRequest.getSdp());

		String answer = null;

		try {
			buildMediaEndPointAndReturnSdp(upStream, downStream);
		} catch (Throwable t) {
			// TODO when final KMS version is ready, perhaps it will be
			// necessary to release httpEndPoint and playerEndPoint resources.
			throw new ContentException(t);
		}

		// We need to assert that session was not rejected while we were
		// creating media infrastructure
		boolean terminate = false;
		synchronized (this) {
			if (state == STATE.TERMINATED) {
				terminate = true;
			} else if (state == STATE.STARTING) {
				state = STATE.ACTIVE;
			}
		}

		// If session was rejected, just terminate
		if (terminate) {
			// clean up
			// return
		} else {
			// If session was not rejected (state=ACTIVE) we send an answer and
			// the initialAsyncCtx becomes useless
			try {
				// Send SDP as answer to client
				protocolManager.sendJsonAnswer(initialAsyncCtx, JsonRpcResponse
						.newStartSdpResponse(answer, sessionId,
								initialJsonRequest.getId()));
				initialAsyncCtx = null;
				initialJsonRequest = null;
			} catch (IOException e) {
				// TODO when final KMS version is ready, perhaps it will be
				// necessary to release httpEndPoint and playerEndPoint
				// resources.
				throw new ContentException(e);
			}
		}
	}
}
