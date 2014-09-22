package org.kurento.client.internal.client.operation;

import org.kurento.client.Continuation;
import org.kurento.client.InternalInfoGetter;
import org.kurento.client.MediaPipeline;
import org.kurento.client.internal.client.DefaultContinuation;
import org.kurento.client.internal.client.RemoteObject;
import org.kurento.client.internal.client.RemoteObjectFacade;
import org.kurento.client.internal.client.RomManager;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient.RequestAndResponseType;

public class MediaPipelineCreationOperation extends Operation {

	private static final String MEDIA_PIPELINE_CLASSNAME = "MediaPipeline";
	private MediaPipeline mediaPipeline;

	public MediaPipelineCreationOperation(MediaPipeline mediaPipeline) {
		this.mediaPipeline = mediaPipeline;
	}

	@Override
	public void exec(RomManager manager) {
		RemoteObject remoteObject = manager.create(MEDIA_PIPELINE_CLASSNAME);
		InternalInfoGetter.setRemoteObject(mediaPipeline, remoteObject);
	}

	@Override
	public void exec(final RomManager manager, final Continuation<Void> cont) {
		manager.create(MEDIA_PIPELINE_CLASSNAME,
				new DefaultContinuation<RemoteObjectFacade>(cont) {
					@Override
					public void onSuccess(RemoteObjectFacade remoteObject)
							throws Exception {
						InternalInfoGetter.setRemoteObject(mediaPipeline,
								remoteObject);
						cont.onSuccess(null);
					}
				});

	}

	@Override
	public RequestAndResponseType createRequest(
			RomClientJsonRpcClient romClientJsonRpcClient) {

		return romClientJsonRpcClient.createCreateRequest(
				MEDIA_PIPELINE_CLASSNAME, null);
	}

	@Override
	public void processResponse(Object response) {
		RemoteObject remoteObject = new RemoteObject((String) response,
				MEDIA_PIPELINE_CLASSNAME, manager);
		InternalInfoGetter.setRemoteObject(mediaPipeline, remoteObject);
	}
}
