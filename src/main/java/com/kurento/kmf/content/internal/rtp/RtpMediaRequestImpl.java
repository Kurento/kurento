package com.kurento.kmf.content.internal.rtp;

import java.io.IOException;

import javax.servlet.AsyncContext;

import org.slf4j.Logger;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.RtpMediaRequest;
import com.kurento.kmf.content.internal.ContentRequestManager;
import com.kurento.kmf.content.internal.base.AbstractSdpBasedMediaRequest;
import com.kurento.kmf.content.internal.jsonrpc.JsonRpcRequest;
import com.kurento.kmf.media.MediaElement;

public class RtpMediaRequestImpl extends AbstractSdpBasedMediaRequest implements
		RtpMediaRequest {

	public RtpMediaRequestImpl(ContentRequestManager manager,
			AsyncContext asyncContext, String contentId) {
		super(manager, asyncContext, contentId);
		// TODO Auto-generated constructor stub
	}

	@Override
	protected String buildMediaEndPointAndReturnSdp(MediaElement upStream,
			MediaElement downStream) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void processStartJsonRpcRequest(AsyncContext asyncCtx,
			JsonRpcRequest message) throws ContentException {
		// TODO Auto-generated method stub

	}

	@Override
	protected Logger getLogger() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void cancelMediaTransmission() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void sendOnTerminateErrorMessageInInitialContext(int code,
			String description) throws IOException {
		// TODO Auto-generated method stub

	}

	@Override
	protected void releaseOwnMediaServerResources() throws Throwable {
		// TODO Auto-generated method stub

	}

}
