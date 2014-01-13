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

import static com.kurento.kms.thrift.api.KmsMediaGStreamerFilterTypeConstants.CONSTRUCTOR_PARAM_GSTREAMER_COMMAND;
import static com.kurento.kms.thrift.api.KmsMediaGStreamerFilterTypeConstants.TYPE_NAME;

import java.util.Map;

import com.kurento.kmf.media.FaceOverlayFilter;
import com.kurento.kmf.media.GStreamerFilter;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.internal.refs.MediaElementRef;
import com.kurento.kmf.media.params.MediaParam;
import com.kurento.kmf.media.params.internal.StringMediaParam;

/**
 * Implementation of the {@link FaceOverlayFilter}
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * 
 * @since 3.0.1
 * 
 */
@ProvidesMediaElement(type = TYPE_NAME)
public class GStreamerFilterImpl extends FilterImpl implements GStreamerFilter {

	/**
	 * Default constructor
	 * 
	 * @param objectRef
	 *            reference object for this filter
	 */
	public GStreamerFilterImpl(MediaElementRef objectRef) {
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
	public GStreamerFilterImpl(MediaElementRef objectRef,
			Map<String, MediaParam> params) {
		super(objectRef, params);
	}

	public static class GStreamerFilterBuilderImpl<T extends GStreamerFilterBuilderImpl<T>>
			extends FilterBuilderImpl<T, GStreamerFilter> implements
			GStreamerFilterBuilder {

		public GStreamerFilterBuilderImpl(final MediaPipeline pipeline,
				String command) {
			super(TYPE_NAME, pipeline);

			StringMediaParam param = new StringMediaParam();
			param.setString(command);

			this.params.put(CONSTRUCTOR_PARAM_GSTREAMER_COMMAND, param);
		}

	}
}
