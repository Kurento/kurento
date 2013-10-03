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
import com.kurento.kmf.media.MediaSink;
import com.kurento.kmf.media.MediaSource;
import com.kurento.kmf.media.internal.refs.MediaPadRef;
import com.kurento.kms.thrift.api.KmsMediaServerException;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient;
import com.kurento.kms.thrift.api.KmsMediaServerService.Client;

public class MediaSinkImpl extends MediaPadImpl implements MediaSink {

	public MediaSinkImpl(MediaPadRef objectRef) {
		super(objectRef);
	}

	@Override
	public void disconnect(MediaSource src) {

		Client client = this.clientPool.acquireSync();

		try {
			client.disconnect(((MediaSourceImpl) src).objectRef.getThriftRef(),
					this.objectRef.getThriftRef());
		} catch (KmsMediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO Change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			this.clientPool.release(client);
		}
	}

	@Override
	public MediaSource getConnectedSrc() {

		Client client = this.clientPool.acquireSync();

		MediaPadRef padRefDTO;
		try {
			padRefDTO = new MediaPadRef(
					client.getConnectedSrc(this.objectRef.getThriftRef()));
		} catch (KmsMediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} catch (TException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			this.clientPool.release(client);
		}

		MediaSourceImpl source = (MediaSourceImpl) ctx.getBean("mediaObject",
				padRefDTO);
		return source;
	}

	@Override
	public void disconnect(MediaSink sink, final Continuation<Void> cont) {
		final AsyncClient client = this.clientPool.acquireAsync();

		try {
			client.disconnect(this.objectRef.getThriftRef(),
					((MediaSinkImpl) sink).objectRef.getThriftRef(),
					new AsyncMethodCallback<AsyncClient.disconnect_call>() {

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							cont.onError(exception);
						}

						@Override
						public void onComplete(
								AsyncClient.disconnect_call response) {
							try {
								response.getResult();
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
							cont.onSuccess(null);
						}
					});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}

	}

	@Override
	public void getConnectedSrc(final Continuation<MediaSource> cont) {
		final AsyncClient client = this.clientPool.acquireAsync();

		try {
			client.getConnectedSrc(
					this.objectRef.getThriftRef(),
					new AsyncMethodCallback<AsyncClient.getConnectedSrc_call>() {

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							cont.onError(exception);
						}

						@Override
						public void onComplete(
								AsyncClient.getConnectedSrc_call response) {
							MediaPadRef srcRef;
							try {
								srcRef = new MediaPadRef(response
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

							MediaSourceImpl src = (MediaSourceImpl) ctx
									.getBean("mediaObject", srcRef);
							cont.onSuccess(src);
						}
					});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}

	}
}
