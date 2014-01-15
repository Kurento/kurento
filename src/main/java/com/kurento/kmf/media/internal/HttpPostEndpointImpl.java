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

import static com.kurento.kms.thrift.api.KmsMediaHttpPostEndPointTypeConstants.TYPE_NAME;

import java.util.Map;

import com.kurento.kmf.media.HttpPostEndpoint;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.internal.refs.MediaElementRef;
import com.kurento.kmf.media.params.MediaParam;

@ProvidesMediaElement(type = TYPE_NAME)
public class HttpPostEndpointImpl extends AbstractHttpEndpoint implements
		HttpPostEndpoint {

	/**
	 * Default constructor
	 * 
	 * @param objectRef
	 *            Reference from KMS
	 */
	public HttpPostEndpointImpl(MediaElementRef objectRef) {
		super(objectRef);
	}

	/**
	 * Constructor that receives the params passed to the KMS, along with the
	 * object to be sued to reference the element
	 * 
	 * @param objectRef
	 *            Reference from KMS
	 * @param params
	 *            used in the construction
	 */
	public HttpPostEndpointImpl(MediaElementRef objectRef,
			Map<String, MediaParam> params) {
		super(objectRef, params);
	}

	static class HttpPostEndpointBuilderImpl<T extends HttpPostEndpointBuilderImpl<T>>
			extends AbstractHttpEndpointBuilderImpl<T, HttpPostEndpoint>
			implements HttpPostEndpointBuilder {

		public HttpPostEndpointBuilderImpl(final MediaPipeline pipeline) {
			super(TYPE_NAME, pipeline);
		}

	}

}
