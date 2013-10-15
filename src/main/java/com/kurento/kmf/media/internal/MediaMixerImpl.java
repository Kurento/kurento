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

import java.util.Map;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.MediaElement;
import com.kurento.kmf.media.MediaMixer;
import com.kurento.kmf.media.internal.refs.MediaElementRef;
import com.kurento.kmf.media.internal.refs.MediaMixerRef;
import com.kurento.kmf.media.params.MediaParam;
import com.kurento.kms.thrift.api.KmsMediaServerException;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.createMixerEndPointWithParams_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.createMixerEndPoint_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.Client;

public class MediaMixerImpl extends AbstractMediaObject implements MediaMixer {

	public MediaMixerImpl(MediaMixerRef objectRef) {
		super(objectRef);
	}

	@Override
	public MediaElement createEndPoint() {

		Client client = clientPool.acquireSync();

		MediaElementRef endPointRef;
		try {
			endPointRef = new MediaElementRef(
					client.createMixerEndPoint(objectRef.getThriftRef()));
		} catch (KmsMediaServerException e) {
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
	public MediaElement createEndPoint(Map<String, MediaParam> params)
			throws KurentoMediaFrameworkException {
		Client client = clientPool.acquireSync();

		MediaElementRef endPointRef;

		try {
			endPointRef = new MediaElementRef(
					client.createMixerEndPointWithParams(
							this.objectRef.getThriftRef(),
							transformMediaParamsMap(params)));
		} catch (KmsMediaServerException e) {
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
							MediaElementRef endPointRef;
							try {
								endPointRef = new MediaElementRef(response
										.getResult());
							} catch (KmsMediaServerException e) {
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
	public void createEndPoint(Map<String, MediaParam> params,
			final Continuation<MediaElement> cont)
			throws KurentoMediaFrameworkException {

		final AsyncClient client = clientPool.acquireAsync();

		try {
			client.createMixerEndPointWithParams(
					objectRef.getThriftRef(),
					transformMediaParamsMap(params),
					new AsyncMethodCallback<createMixerEndPointWithParams_call>() {

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							cont.onError(exception);
						}

						@Override
						public void onComplete(
								createMixerEndPointWithParams_call response) {
							MediaElementRef endPointRef;
							try {
								endPointRef = new MediaElementRef(response
										.getResult());
							} catch (KmsMediaServerException e) {
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
