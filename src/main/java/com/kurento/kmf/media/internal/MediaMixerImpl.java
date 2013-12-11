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

import static com.kurento.kms.thrift.api.KmsMediaJackVaderFilterTypeConstants.TYPE_NAME;

import java.util.Map;

import org.apache.thrift.TException;
import org.apache.thrift.async.AsyncMethodCallback;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.MediaElement;
import com.kurento.kmf.media.MediaMixer;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.internal.refs.MediaElementRef;
import com.kurento.kmf.media.internal.refs.MediaMixerRef;
import com.kurento.kmf.media.params.MediaParam;
import com.kurento.kmf.media.params.internal.MediaObjectConstructorParam;
import com.kurento.kms.thrift.api.KmsMediaServerException;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.createMixerEndPointWithParams_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.createMixerEndPoint_call;
import com.kurento.kms.thrift.api.KmsMediaServerService.Client;

public class MediaMixerImpl extends AbstractCollectableMediaObject implements
		MediaMixer {

	/**
	 * Constructor that configures, by default, the mixer as a non-collectable
	 * object: No keepalives will be sent to the media server, and the
	 * collection of this object by the JVM will NOT imply a destruction of the
	 * object in the server.
	 * 
	 * The object created by this means, will be associated with the life cycle
	 * of the enclosing {@link MediaPipeline}. In order to release the element,
	 * the user can invoke {@link MediaMixer#release()}
	 * 
	 * @param objectRef
	 *            mixer reference
	 */
	public MediaMixerImpl(MediaMixerRef objectRef) {
		super(objectRef, 0);
	}

	/**
	 * Constructor with parameters to be sent to the media server. The entries
	 * in the map will be used by the server to configure the object while
	 * creating it.
	 * 
	 * If no garbage period is configured, using the structure
	 * {@link MediaObjectConstructorParam}, the object will NOT be collected in
	 * the media server, and will be associated with the life cycle of the
	 * enclosing {@link MediaPipeline}. In order to release the element, the
	 * user can invoke {@link MediaMixer#release()}
	 * 
	 * @param ref
	 *            mixer reference
	 * @param params
	 *            map of parameters. The key is the name of the parameter, while
	 *            the value represents the param itself.
	 */
	public MediaMixerImpl(MediaMixerRef ref, Map<String, MediaParam> params) {
		super(ref, setDefaultGarbagePeriodParam(params, 0));
	}

	@Override
	public MediaElement createEndpoint() {

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
	public MediaElement createEndpoint(Map<String, MediaParam> params)
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
				"mediaObjectWithParams", endPointRef, params);
		return endPoint;
	}

	@Override
	public void createEndpoint(final Continuation<MediaElement> cont)
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
	public void createEndpoint(final Map<String, MediaParam> params,
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
									.getBean("mediaObjectWithParams",
											endPointRef, params);
							cont.onSuccess(endPoint);
						}
					});
		} catch (TException e) {
			clientPool.release(client);
			// TODO change error code
			throw new KurentoMediaFrameworkException(e.getMessage(), e, 30000);
		}
	}

	static class MediaMixerBuilderImpl<T extends MediaMixerBuilderImpl<T, E>, E extends MediaMixer>
			extends AbstractCollectableMediaObjectBuilder<T, E> {

		/**
		 * @param elementType
		 */
		protected MediaMixerBuilderImpl(final String elementType,
				final MediaPipeline pipeline) {
			super(elementType, pipeline, 0);
		}

		public MediaMixerBuilderImpl(final MediaPipeline pipeline) {
			this(TYPE_NAME, pipeline);
		}

		@SuppressWarnings("unchecked")
		@Override
		public E build() {
			return (E) pipeline.createMediaMixer(elementName, params);
		}

		@Override
		public void buildAsync(Continuation<E> cont) {
			pipeline.createMediaMixer(elementName, params, cont);
		}

	}

}
