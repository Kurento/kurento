/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package com.kurento.kmf.media;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.commands.MediaParams;
import com.kurento.kmf.media.commands.internal.AbstractMediaParams;
import com.kurento.kmf.media.internal.MediaPipelineImpl;
import com.kurento.kmf.media.internal.pool.MediaServerClientPoolService;
import com.kurento.kmf.media.internal.refs.MediaPipelineRefDTO;
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

	public MediaPipeline create() {
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

		MediaPipelineImpl pipeline = (MediaPipelineImpl) ctx.getBean(
				"mediaPipeline", pipelineRefDTO);
		return pipeline;
	}

	public MediaPipeline create(MediaParams params)
			throws KurentoMediaFrameworkException {

		Client client = this.clientPool.acquireSync();

		MediaPipelineRefDTO pipelineRefDTO;
		try {
			pipelineRefDTO = new MediaPipelineRefDTO(
					client.createMediaPipelineWithParams(((AbstractMediaParams) params)
							.getThriftParams()));
		} catch (MediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			this.clientPool.release(client);
		}

		MediaPipelineImpl pipeline = (MediaPipelineImpl) ctx.getBean(
				"mediaPipeline", pipelineRefDTO);
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
					MediaPipelineImpl pipeline = (MediaPipelineImpl) ctx
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

	public void create(MediaParams params,
			final Continuation<MediaPipeline> cont)
			throws KurentoMediaFrameworkException {

		final AsyncClient client = this.clientPool.acquireAsync();

		try {
			client.createMediaPipelineWithParams(
					((AbstractMediaParams) params).getThriftParams(),
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
							MediaPipelineImpl pipeline = (MediaPipelineImpl) ctx
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
