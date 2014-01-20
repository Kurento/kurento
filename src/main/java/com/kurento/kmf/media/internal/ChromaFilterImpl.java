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

import static com.google.common.collect.Maps.newHashMapWithExpectedSize;
import static com.kurento.kms.thrift.api.KmsMediaChromaFilterTypeConstants.CONSTRUCTOR_PARAMS_DATA_TYPE;
import static com.kurento.kms.thrift.api.KmsMediaChromaFilterTypeConstants.SET_BACKGROUND;
import static com.kurento.kms.thrift.api.KmsMediaChromaFilterTypeConstants.SET_BACKGROUND_PARAM_BACKGROUND_IMAGE;
import static com.kurento.kms.thrift.api.KmsMediaChromaFilterTypeConstants.TYPE_NAME;
import static com.kurento.kms.thrift.api.KmsMediaChromaFilterTypeConstants.UNSET_BACKGROUND;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.media.ChromaFilter;
import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.FaceOverlayFilter;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.internal.refs.MediaElementRef;
import com.kurento.kmf.media.params.MediaParam;
import com.kurento.kmf.media.params.internal.ChromaBackgroundImageParam;
import com.kurento.kmf.media.params.internal.ChromaConstructorParam;
import com.kurento.kmf.media.params.internal.WindowParam;

/**
 * Implementation of the {@link FaceOverlayFilter}
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * 
 * @since 3.0.1
 * 
 */
@ProvidesMediaElement(type = TYPE_NAME)
public class ChromaFilterImpl extends FilterImpl implements ChromaFilter {

	/**
	 * Default constructor
	 * 
	 * @param objectRef
	 *            reference object for this filter
	 */
	public ChromaFilterImpl(MediaElementRef objectRef) {
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
	public ChromaFilterImpl(MediaElementRef objectRef,
			Map<String, MediaParam> params) {
		super(objectRef, params);
	}

	@Override
	public void setBackground(String uri) {
		try {
			URI uriObj = new URI(uri);
			setBackground(uriObj);
		} catch (URISyntaxException e) {
			// TODO add error code
			throw new KurentoMediaFrameworkException(
					"The string received as uri does not have the correct syntax",
					e, 30000);
		}
	}

	@Override
	public void setBackground(URI uri) {
		Map<String, MediaParam> params = newHashMapWithExpectedSize(1);
		ChromaBackgroundImageParam param = new ChromaBackgroundImageParam();
		param.setImageUri(uri);
		params.put(SET_BACKGROUND_PARAM_BACKGROUND_IMAGE, param);
		this.invoke(SET_BACKGROUND, params);
	}

	@Override
	public void unsetBackground() {
		this.invoke(UNSET_BACKGROUND);
	}

	@Override
	public void unsetBackground(Continuation<Void> cont) {
		this.invoke(UNSET_BACKGROUND, new VoidContinuationWrapper(cont));
	}

	@Override
	public void setBackground(String uri, Continuation<Void> cont) {
		try {
			URI uriObj = new URI(uri);
			setBackground(uriObj, cont);
		} catch (URISyntaxException e) {
			// TODO add error code
			throw new KurentoMediaFrameworkException(
					"The string received as uri does not have the correct syntax",
					e, 30000);
		}
	}

	@Override
	public void setBackground(URI uri, Continuation<Void> cont) {
		Map<String, MediaParam> params = newHashMapWithExpectedSize(1);
		ChromaBackgroundImageParam param = new ChromaBackgroundImageParam();
		param.setImageUri(uri);
		params.put(SET_BACKGROUND_PARAM_BACKGROUND_IMAGE, param);
		this.invoke(SET_BACKGROUND, params, new VoidContinuationWrapper(cont));
	}

	public static class ChromaFilterBuilderImpl<T extends ChromaFilterBuilderImpl<T>>
			extends FilterBuilderImpl<T, ChromaFilter> implements
			ChromaFilterBuilder {

		private final ChromaConstructorParam param;

		public ChromaFilterBuilderImpl(final MediaPipeline pipeline,
				final WindowParam window) {
			super(TYPE_NAME, pipeline);
			param = new ChromaConstructorParam.Builder(window).build();
			params.put(CONSTRUCTOR_PARAMS_DATA_TYPE, param);
		}

		@Override
		public ChromaFilterBuilder withBackgroundImage(String uri)
				throws URISyntaxException {
			param.setImageUri(uri);
			return self();
		}

		@Override
		public ChromaFilterBuilder withBackgroundImage(URI uri) {
			param.setImageUri(uri);
			return self();
		}
	}

}
