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

import static com.kurento.kms.thrift.api.KmsMediaHttpGetEndPointTypeConstants.CONSTRUCTOR_PARAMS_DATA_TYPE;
import static com.kurento.kms.thrift.api.KmsMediaHttpGetEndPointTypeConstants.EVENT_EOS_DETECTED;
import static com.kurento.kms.thrift.api.KmsMediaHttpGetEndPointTypeConstants.TYPE_NAME;

import java.util.Map;

import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.HttpGetEndpoint;
import com.kurento.kmf.media.ListenerRegistration;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.MediaProfileSpecType;
import com.kurento.kmf.media.events.EndOfStreamEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.internal.refs.MediaElementRef;
import com.kurento.kmf.media.params.MediaParam;
import com.kurento.kmf.media.params.internal.HttpGetEndpointConstructorParam;

@ProvidesMediaElement(type = TYPE_NAME)
public class HttpGetEndpointImpl extends AbstractHttpEndpoint implements
		HttpGetEndpoint {

	public HttpGetEndpointImpl(MediaElementRef endpointRef) {
		super(endpointRef);
	}

	/**
	 * @param objectRef
	 * @param params
	 */
	public HttpGetEndpointImpl(MediaElementRef objectRef,
			Map<String, MediaParam> params) {
		super(objectRef, params);
	}

	/* SYNC */
	@Override
	public ListenerRegistration addEndOfStreamListener(
			MediaEventListener<EndOfStreamEvent> listener) {
		return addListener(EVENT_EOS_DETECTED, listener);
	}

	/* ASYNC */
	@Override
	public void addEndOfStreamListener(
			MediaEventListener<EndOfStreamEvent> listener,
			Continuation<ListenerRegistration> cont) {
		addListener(EVENT_EOS_DETECTED, listener, cont);
	}

	static class HttpGetEndpointBuilderImpl<T extends HttpGetEndpointBuilderImpl<T>>
			extends AbstractHttpEndpointBuilderImpl<T, HttpGetEndpoint>
			implements HttpGetEndpointBuilder {

		// The param is not stored in the map of params until some constructor
		// param is set.
		private final HttpGetEndpointConstructorParam param = new HttpGetEndpointConstructorParam();

		public HttpGetEndpointBuilderImpl(final MediaPipeline pipeline) {
			super(TYPE_NAME, pipeline);
		}

		@Override
		public T terminateOnEOS() {
			param.setTerminateOnEOS(Boolean.TRUE);
			params.put(CONSTRUCTOR_PARAMS_DATA_TYPE, param);
			return self();
		}

		@Override
		public T withMediaProfile(MediaProfileSpecType type) {
			param.setMediaMuxer(type.toThrift());
			params.put(CONSTRUCTOR_PARAMS_DATA_TYPE, param);
			return self();
		}

	}

}
