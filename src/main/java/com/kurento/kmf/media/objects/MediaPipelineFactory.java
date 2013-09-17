package com.kurento.kmf.media.objects;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.internal.refs.MediaPipelineRefDTO;
import com.kurento.kmf.media.pool.MediaServerClientPoolService;
import com.kurento.kms.thrift.api.Command;
import com.kurento.kms.thrift.api.MediaServerException;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient.createMediaPipelineWithParams_call;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient.createMediaPipeline_call;
import com.kurento.kms.thrift.api.MediaServerService.Client;

public class MediaPipelineFactory {

	@Autowired
	private MediaServerClientPoolService clientPool;

	@Autowired
	private ApplicationContext ctx;

	public MediaPipeline create() throws KurentoMediaFrameworkException {
		Client client = this.clientPool.acquireSync();

		MediaPipelineRefDTO pipelineRefDTO;
		try {
			pipelineRefDTO = new MediaPipelineRefDTO(
					client.createMediaPipeline());
		} catch (MediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			this.clientPool.release(client);
		}

		MediaPipeline pipeline = (MediaPipeline) ctx.getBean("mediaPipeline",
				pipelineRefDTO);
		return pipeline;
	}

	public MediaPipeline create(Command params)
			throws KurentoMediaFrameworkException {
		Client client = this.clientPool.acquireSync();

		MediaPipelineRefDTO pipelineRefDTO;
		try {
			pipelineRefDTO = new MediaPipelineRefDTO(
					client.createMediaPipelineWithParams(params));
		} catch (MediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			this.clientPool.release(client);
		}

		MediaPipeline pipeline = (MediaPipeline) ctx.getBean("mediaPipeline",
				pipelineRefDTO);
		return pipeline;
	}

	public void create(final Continuation<MediaPipeline> cont)
			throws KurentoMediaFrameworkException {
		final AsyncClient client = this.clientPool.acquireAsync();

		try {
			client.createMediaPipeline(new AsyncMethodCallback<createMediaPipeline_call>() {

				@Override
				public void onError(Exception exception) {
					clientPool.release(client);
				}

				@Override
				public void onComplete(createMediaPipeline_call response) {
					MediaPipelineRefDTO pipelineRefDTO;
					try {
						pipelineRefDTO = new MediaPipelineRefDTO(response
								.getResult());
					} catch (MediaServerException e) {
						throw new KurentoMediaFrameworkException(
								e.getMessage(), e, e.getErrorCode());
					} catch (TException e) {
						// TODO change error code
						throw new KurentoMediaFrameworkException(
								e.getMessage(), e, 30000);
					} finally {
						clientPool.release(client);
					}
					MediaPipeline pipeline = (MediaPipeline) ctx.getBean(
							"mediaPipeline", pipelineRefDTO);
					cont.onSuccess(pipeline);
				}
			});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}

	}

	public void create(Command params, final Continuation<MediaPipeline> cont)
			throws KurentoMediaFrameworkException {
		final AsyncClient client = this.clientPool.acquireAsync();

		try {
			client.createMediaPipelineWithParams(
					params,
					new AsyncMethodCallback<createMediaPipelineWithParams_call>() {

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
						}

						@Override
						public void onComplete(
								createMediaPipelineWithParams_call response) {
							MediaPipelineRefDTO pipelineRefDTO;
							try {
								pipelineRefDTO = new MediaPipelineRefDTO(
										response.getResult());
							} catch (MediaServerException e) {
								throw new KurentoMediaFrameworkException(e
										.getMessage(), e, e.getErrorCode());
							} catch (TException e) {
								// TODO change error code
								throw new KurentoMediaFrameworkException(e
										.getMessage(), e, 30000);
							} finally {
								clientPool.release(client);
							}
							MediaPipeline pipeline = (MediaPipeline) ctx
									.getBean("mediaPipeline", pipelineRefDTO);
							cont.onSuccess(pipeline);
						}
					});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}

	}

}
