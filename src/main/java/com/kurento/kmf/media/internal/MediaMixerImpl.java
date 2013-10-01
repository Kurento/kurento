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
package com.kurento.kmf.media.internal;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.MediaElement;
import com.kurento.kmf.media.MediaMixer;
import com.kurento.kmf.media.commands.MediaParams;
import com.kurento.kmf.media.commands.internal.AbstractMediaParams;
import com.kurento.kmf.media.internal.refs.MediaElementRefDTO;
import com.kurento.kmf.media.internal.refs.MediaMixerRefDTO;
import com.kurento.kms.thrift.api.MediaServerException;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient.createMixerEndPointWithParams_call;
import com.kurento.kms.thrift.api.MediaServerService.AsyncClient.createMixerEndPoint_call;
import com.kurento.kms.thrift.api.MediaServerService.Client;

public class MediaMixerImpl extends AbstractMediaObject implements MediaMixer {

	public MediaMixerImpl(MediaMixerRefDTO objectRef) {
		super(objectRef);
	}

	@Override
	public MediaElement createEndPoint() throws KurentoMediaFrameworkException {

		Client client = clientPool.acquireSync();

		MediaElementRefDTO endPointRef;
		try {
			endPointRef = new MediaElementRefDTO(
					client.createMixerEndPoint(objectRef.getThriftRef()));
		} catch (MediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			clientPool.release(client);
		}

		MediaElementImpl endPoint = (MediaElementImpl) ctx.getBean(
				"mediaObject", endPointRef);
		return endPoint;
	}

	@Override
	public MediaElement createEndPoint(MediaParams params)
			throws KurentoMediaFrameworkException {
		Client client = clientPool.acquireSync();

		MediaElementRefDTO endPointRef;

		try {
			endPointRef = new MediaElementRefDTO(
					client.createMixerEndPointWithParams(
							this.objectRef.getThriftRef(),
							((AbstractMediaParams) params).getThriftParams()));
		} catch (MediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			clientPool.release(client);
		}

		MediaElementImpl endPoint = (MediaElementImpl) ctx.getBean(
				"mediaObject", endPointRef);
		return endPoint;
	}

	@Override
	public void createEndPoint(final Continuation<MediaElement> cont)
			throws KurentoMediaFrameworkException {
		final AsyncClient client = clientPool.acquireAsync();

		try {
			client.createMixerEndPoint(objectRef.getThriftRef(),
					new AsyncMethodCallback<createMixerEndPoint_call>() {

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							cont.onError(exception);
						}

						@Override
						public void onComplete(createMixerEndPoint_call response) {
							MediaElementRefDTO endPointRef;
							try {
								endPointRef = new MediaElementRefDTO(response
										.getResult());
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
							MediaElementImpl endPoint = (MediaElementImpl) ctx
									.getBean("mediaObject", endPointRef);
							cont.onSuccess(endPoint);
						}
					});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}

	}

	@Override
	public void createEndPoint(MediaParams params,
			final Continuation<MediaElement> cont)
			throws KurentoMediaFrameworkException {

		final AsyncClient client = clientPool.acquireAsync();

		try {
			client.createMixerEndPointWithParams(
					objectRef.getThriftRef(),
					((AbstractMediaParams) params).getThriftParams(),
					new AsyncMethodCallback<createMixerEndPointWithParams_call>() {

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							cont.onError(exception);
						}

						@Override
						public void onComplete(
								createMixerEndPointWithParams_call response) {
							MediaElementRefDTO endPointRef;
							try {
								endPointRef = new MediaElementRefDTO(response
										.getResult());
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
							MediaElementImpl endPoint = (MediaElementImpl) ctx
									.getBean("mediaObject", endPointRef);
							cont.onSuccess(endPoint);
						}
					});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}
	}
}
