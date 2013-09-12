package com.kurento.kmf.content.internal.recorder;

import javax.servlet.AsyncContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.RecordRequest;
import com.kurento.kmf.content.RecorderHandler;
import com.kurento.kmf.content.internal.ContentRequestManager;
import com.kurento.kmf.content.internal.base.AbstractHttpBasedContentRequest;
import com.kurento.kmf.content.jsonrpc.JsonRpcRequest;
import com.kurento.kmf.media.HttpEndPoint;
import com.kurento.kmf.media.MediaElement;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.RecorderEndPoint;

/**
 * 
 * Request implementation for a Recorder.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @version 1.0.0
 */
public class RecordRequestImpl extends AbstractHttpBasedContentRequest
		implements RecordRequest {

	/**
	 * Logger.
	 */
	private static final Logger log = LoggerFactory
			.getLogger(RecordRequestImpl.class);

	/**
	 * Recorder Handler reference.
	 */
	private RecorderHandler handler;

	/**
	 * Parameterized constructor.
	 * 
	 * @param handler
	 *            Recorder Handler
	 * @param manager
	 *            Content Request Manager
	 * @param asyncContext
	 *            Asynchronous context
	 * @param contentId
	 *            Content identifier
	 * @param redirect
	 *            Redirect strategy
	 * @param useControlProtocol
	 *            JSON-based signaling protocol strategy
	 */
	public RecordRequestImpl(RecorderHandler handler,
			ContentRequestManager manager, AsyncContext asyncContext,
			String contentId, boolean redirect, boolean useControlProtocol) {
		super(manager, asyncContext, contentId, redirect, useControlProtocol);
		this.handler = handler;
	}

	/**
	 * Recorder handler accessor (getter).
	 * 
	 * @return Recorder handler
	 */
	public RecorderHandler getHandler() {
		return handler;
	}

	/**
	 * Perform record action using a ContentPath.
	 * 
	 */
	@Override
	public void record(String contentPath) throws ContentException {
		activateMedia(null, contentPath);
	}

	/**
	 * Perform a record action using a MediaElement.
	 * 
	 * @param element
	 *            Pluggable media component
	 */
	@Override
	public void record(MediaElement element) throws ContentException {
		activateMedia(element, null);
	}

	/**
	 * Creates a Media Element repository using a ContentPath.
	 * 
	 */
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

	/**
	 * Creates a Media Element repository using a MediaElement.
	 */
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

	/**
	 * Performs then onRecordRequest event of the Handler.
	 */
	@Override
	protected void processStartJsonRpcRequest(AsyncContext asyncCtx,
			JsonRpcRequest message) throws ContentException {
		handler.onRecordRequest(this);
	}

	/**
	 * Logger accessor (getter).
	 */
	@Override
	protected Logger getLogger() {
		return log;
	}

	/**
	 * Cancel of media transmission.
	 */
	@Override
	protected void cancelMediaTransmission() {
		// TODO Auto-generated method stub
	}
}
