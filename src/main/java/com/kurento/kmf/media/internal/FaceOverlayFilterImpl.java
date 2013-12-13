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

import static com.kurento.kms.thrift.api.KmsMediaFaceOverlayFilterTypeConstants.SET_IMAGE_OVERLAY;
import static com.kurento.kms.thrift.api.KmsMediaFaceOverlayFilterTypeConstants.SET_IMAGE_OVERLAY_PARAM_IMAGE;
import static com.kurento.kms.thrift.api.KmsMediaFaceOverlayFilterTypeConstants.TYPE_NAME;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.FaceOverlayFilter;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.internal.refs.MediaElementRef;
import com.kurento.kmf.media.params.MediaParam;
import com.kurento.kmf.media.params.internal.FaceOverlayImageParam;

/**
 * Implementation of the {@link FaceOverlayFilter}
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * 
 * @since 2.0.1
 * 
 */
@ProvidesMediaElement(type = TYPE_NAME)
public class FaceOverlayFilterImpl extends FilterImpl implements
		FaceOverlayFilter {

	/**
	 * Default constructor
	 * 
	 * @param objectRef
	 *            reference object for this filter
	 */
	public FaceOverlayFilterImpl(MediaElementRef objectRef) {
		super(objectRef);
	}

	/**
	 * Constructor with params
	 * 
	 * @param objectRef
	 *            reference object for this filter
	 * @param params
	 *            parameters to be passed to constructors
	 */
	public FaceOverlayFilterImpl(MediaElementRef objectRef,
			Map<String, MediaParam> params) {
		super(objectRef, params);
	}

	public static class FaceOverlayFilterBuilderImpl<T extends FaceOverlayFilterBuilderImpl<T>>
			extends FilterBuilderImpl<T, FaceOverlayFilter> implements
			FaceOverlayFilterBuilder {

		public FaceOverlayFilterBuilderImpl(final MediaPipeline pipeline) {
			super(TYPE_NAME, pipeline);
		}

	}

	@Override
	public void setOverlayedImage(URI uri, float offsetXPercent,
			float offsetYPercent, float widthPercent, float heightPercent) {
		FaceOverlayImageParam param = new FaceOverlayImageParam(uri,
				offsetXPercent, offsetYPercent, widthPercent, heightPercent);
		Map<String, MediaParam> params = new HashMap<String, MediaParam>(4);
		params.put(SET_IMAGE_OVERLAY_PARAM_IMAGE, param);
		this.invoke(SET_IMAGE_OVERLAY, params);
	}

	@Override
	public void setOverlayedImage(String uri, float offsetXPercent,
			float offsetYPercent, float widthPercent, float heightPercent) {

		try {
			FaceOverlayImageParam param = new FaceOverlayImageParam(uri,
					offsetXPercent, offsetYPercent, widthPercent, heightPercent);
			Map<String, MediaParam> params = new HashMap<String, MediaParam>(4);
			params.put(SET_IMAGE_OVERLAY_PARAM_IMAGE, param);
			this.invoke(SET_IMAGE_OVERLAY, params);
		} catch (URISyntaxException e) {
			// TODO add error code
			throw new KurentoMediaFrameworkException(
					"The string received as uri does not have the correct syntax",
					e, 30000);
		}
	}

	@Override
	public void setOverlayedImage(URI uri, float offsetXPercent,
			float offsetYPercent, float widthPercent, float heightPercent,
			Continuation<Void> cont) {
		FaceOverlayImageParam param = new FaceOverlayImageParam(uri,
				offsetXPercent, offsetYPercent, widthPercent, heightPercent);
		Map<String, MediaParam> params = new HashMap<String, MediaParam>(4);
		params.put(SET_IMAGE_OVERLAY_PARAM_IMAGE, param);
		this.invoke(SET_IMAGE_OVERLAY, params,
				new VoidContinuationWrapper(cont));

	}

	@Override
	public void setOverlayedImage(String uriStr, float offsetXPercent,
			float offsetYPercent, float widthPercent, float heightPercent,
			Continuation<Void> cont) {
		try {
			URI uri = new URI(uriStr);
			this.setOverlayedImage(uri, offsetXPercent, offsetYPercent,
					widthPercent, heightPercent, cont);
		} catch (URISyntaxException e) {
			// TODO add error code
			throw new KurentoMediaFrameworkException(
					"The string received as uri does not have the correct syntax",
					e, 30000);
		}
	}

}
