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

import static com.kurento.kms.thrift.api.KmsMediaPlateDetectorFilterTypeConstants.EVENT_PLATE_DETECTED;
import static com.kurento.kms.thrift.api.KmsMediaPlateDetectorFilterTypeConstants.TYPE_NAME;

import java.util.Map;

import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.ListenerRegistration;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PlateDetectorFilter;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.events.PlateDetectedEvent;
import com.kurento.kmf.media.internal.refs.MediaElementRef;
import com.kurento.kmf.media.params.MediaParam;

@ProvidesMediaElement(type = TYPE_NAME)
public class PlateDetectorFilterImpl extends FilterImpl implements
		PlateDetectorFilter {

	public PlateDetectorFilterImpl(MediaElementRef filterId) {
		super(filterId);
	}

	public PlateDetectorFilterImpl(MediaElementRef objectRef,
			Map<String, MediaParam> params) {
		super(objectRef, params);
	}

	@Override
	public ListenerRegistration addPlateDetectedListener(
			MediaEventListener<PlateDetectedEvent> listener) {
		return addListener(EVENT_PLATE_DETECTED, listener);
	}

	@Override
	public void addPlateDetectedListener(
			MediaEventListener<PlateDetectedEvent> listener,
			Continuation<ListenerRegistration> cont) {
		addListener(EVENT_PLATE_DETECTED, listener, cont);
	}

	public static class PlateDetectorFilterBuilderImpl<T extends PlateDetectorFilterBuilderImpl<T>>
			extends FilterBuilderImpl<T, PlateDetectorFilter> implements
			PlateDetectorFilterBuilder {

		public PlateDetectorFilterBuilderImpl(final MediaPipeline pipeline) {
			super(TYPE_NAME, pipeline);
		}

	}

}
