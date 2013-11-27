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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.MediaSink;
import com.kurento.kmf.media.MediaSource;
import com.kurento.kmf.media.internal.refs.MediaPadRef;
import com.kurento.kms.thrift.api.KmsMediaObjectRef;
import com.kurento.kms.thrift.api.KmsMediaServerException;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient;
import com.kurento.kms.thrift.api.KmsMediaServerService.Client;

public class MediaSourceImpl extends MediaPadImpl implements MediaSource {

	public MediaSourceImpl(MediaPadRef objectRef) {
		super(objectRef);
	}

	@Override
	public void connect(MediaSink sink) {

		final Client client = clientPool.acquireSync();

		try {
			client.connect(objectRef.getThriftRef(),
					((MediaSinkImpl) sink).objectRef.getThriftRef());
		} catch (KmsMediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			clientPool.release(client);
		}
	}

	@Override
	public Collection<MediaSink> getConnectedSinks() {
		final Client client = clientPool.acquireSync();

		List<KmsMediaObjectRef> sinkRefs;

		try {
			sinkRefs = client.getConnectedSinks(objectRef.getThriftRef());
		} catch (KmsMediaServerException e) {
			throw new KurentoMediaFrameworkException(e.getMessage(), e,
					e.getErrorCode());
		} catch (TException e) {
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		} finally {
			clientPool.release(client);
		}

		return createMediaSinks(sinkRefs);
	}

	@Override
	public void connect(MediaSink sink, final Continuation<Void> cont) {
		final AsyncClient client = clientPool.acquireAsync();

		try {
			client.connect(objectRef.getThriftRef(),
					((MediaSinkImpl) sink).objectRef.getThriftRef(),
					new AsyncMethodCallback<AsyncClient.connect_call>() {

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
						}

						@Override
						public void onComplete(AsyncClient.connect_call response) {
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
	public void getConnectedSinks(final Continuation<Collection<MediaSink>> cont) {
		final AsyncClient client = clientPool.acquireAsync();

		try {
			client.getConnectedSinks(
					objectRef.getThriftRef(),
					new AsyncMethodCallback<AsyncClient.getConnectedSinks_call>() {

						@Override
						public void onError(Exception exception) {
							clientPool.release(client);
							cont.onError(exception);
						}

						@Override
						public void onComplete(
								AsyncClient.getConnectedSinks_call response) {
							List<KmsMediaObjectRef> sinkRefs;
							try {
								sinkRefs = response.getResult();
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

							cont.onSuccess(createMediaSinks(sinkRefs));
						}
					});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}
	}

	private Collection<MediaSink> createMediaSinks(
			Collection<KmsMediaObjectRef> sinkRefs) {
		Collection<MediaSink> sinks = new ArrayList<MediaSink>(sinkRefs.size());
		for (KmsMediaObjectRef padRef : sinkRefs) {
			MediaSinkImpl sink = (MediaSinkImpl) ctx.getBean("mediaObject",
					new MediaPadRef(padRef));
			sinks.add(sink);
		}

		return sinks;
	}

}
