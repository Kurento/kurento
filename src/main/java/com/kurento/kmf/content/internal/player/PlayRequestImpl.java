package com.kurento.kmf.content.internal.player;

import javax.servlet.AsyncContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.content.ContentException;
import com.kurento.kmf.content.PlayRequest;
import com.kurento.kmf.content.PlayerHandler;
import com.kurento.kmf.content.internal.ContentRequestManager;
import com.kurento.kmf.content.internal.base.AbstractHttpBasedContentRequest;
import com.kurento.kmf.content.internal.jsonrpc.JsonRpcRequest;
import com.kurento.kmf.media.HttpEndPoint;
import com.kurento.kmf.media.MediaElement;

public class PlayRequestImpl extends AbstractHttpBasedContentRequest implements
		PlayRequest {

	private static final Logger log = LoggerFactory
			.getLogger(PlayRequestImpl.class);

	private PlayerHandler handler;

	public PlayRequestImpl(PlayerHandler handler,
			ContentRequestManager manager, AsyncContext asyncContext,
			String contentId, boolean redirect, boolean useControlProtocol) {
		super(manager, asyncContext, contentId, redirect, useControlProtocol);
		this.handler = handler;
	}

	public PlayerHandler getHandler() {
		return handler;
	}

	@Override
	public void play(String contentPath) throws ContentException {
		activateMedia(null, contentPath);

	}

	@Override
	public void play(MediaElement element) throws ContentException {
		activateMedia(element, null);
	}

	@Override
	protected MediaElement buildRepositoryBasedMediaElement(String contentPath) {
		// TODO Build player
		return null;
	}

	@Override
	protected HttpEndPoint buildHttpEndPointMediaElement(
			MediaElement mediaElement) {
		// TODO Build httpEndpoint in MediaPipeline of the provided mediaElement
		// and chain to the provided media element
		return null;
	}

	@Override
	protected void processStartJsonRpcRequest(AsyncContext asyncCtx,
			JsonRpcRequest message) throws ContentException {
		handler.onPlayRequest(this);
	}

	@Override
	protected Logger getLogger() {
		return log;
	}

	@Override
	protected void cancelMediaTransmission() {
		// TODO Auto-generated method stub

	}

	@Override
	protected void releaseOwnMediaServerResources() throws Throwable {
		// TODO Auto-generated method stub
	}
}
