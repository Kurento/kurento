package com.kurento.kmf.content.internal.recorder;

import javax.servlet.AsyncContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.RecordRequest;
import com.kurento.kmf.content.RecorderHandler;
import com.kurento.kmf.content.internal.ContentRequestManager;
import com.kurento.kmf.content.internal.base.AbstractHttpBasedContentRequest;
import com.kurento.kmf.content.internal.jsonrpc.JsonRpcRequest;
import com.kurento.kmf.media.HttpEndPoint;
import com.kurento.kmf.media.MediaElement;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.RecorderEndPoint;

public class RecordRequestImpl extends AbstractHttpBasedContentRequest
		implements RecordRequest {

	private static final Logger log = LoggerFactory
			.getLogger(RecordRequestImpl.class);

	private RecorderHandler handler;

	public RecordRequestImpl(RecorderHandler handler,
			ContentRequestManager manager, AsyncContext asyncContext,
			String contentId, boolean redirect, boolean useControlProtocol) {
		super(manager, asyncContext, contentId, redirect, useControlProtocol);
		this.handler = handler;
	}

	public RecorderHandler getHandler() {
		return handler;
	}

	@Override
	public void record(String contentPath) throws ContentException {
		activateMedia(null, contentPath);
	}

	@Override
	public void record(MediaElement element) throws ContentException {
		activateMedia(element, null);
	}

	@Override
	protected MediaElement buildRepositoryBasedMediaElement(String contentPath)
			throws Exception {
		getLogger().info("Creating media pipeline ...");
		MediaPipeline mediaPipeline = mediaPipelineFactory
				.createMediaPipeline();
		addForCleanUp(mediaPipeline);
		getLogger().info("Creating RecorderEndPoint ...");
		RecorderEndPoint recorderEndPoint = mediaPipeline.createUriEndPoint(
				RecorderEndPoint.class, contentPath);
		recorderEndPoint.record();
		return recorderEndPoint;
	}

	@Override
	protected HttpEndPoint buildAndConnectHttpEndPointMediaElement(
			MediaElement mediaElement) throws Exception {
		MediaPipeline mediaPiplePipeline = mediaElement.getMediaPipeline();
		getLogger().info("Creating HttpEndPoint ...");
		HttpEndPoint httpEndPoint = mediaPiplePipeline.createHttpEndPoint();
		addForCleanUp(httpEndPoint);
		connect(httpEndPoint, mediaElement);
		return httpEndPoint;
	}

	@Override
	protected void processStartJsonRpcRequest(AsyncContext asyncCtx,
			JsonRpcRequest message) throws ContentException {
		handler.onRecordRequest(this);

	}

	@Override
	protected Logger getLogger() {
		return log;
	}

	@Override
	protected void cancelMediaTransmission() {
		// TODO Auto-generated method stub
	}
}
