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
import static com.kurento.kms.thrift.api.KmsMediaPointerDetector2FilterTypeConstants.ADD_NEW_WINDOW;
import static com.kurento.kms.thrift.api.KmsMediaPointerDetector2FilterTypeConstants.ADD_NEW_WINDOW_PARAM_WINDOW;
import static com.kurento.kms.thrift.api.KmsMediaPointerDetector2FilterTypeConstants.CLEAR_WINDOWS;
import static com.kurento.kms.thrift.api.KmsMediaPointerDetector2FilterTypeConstants.CONSTRUCTOR_PARAMS_DATA_TYPE;
import static com.kurento.kms.thrift.api.KmsMediaPointerDetector2FilterTypeConstants.EVENT_WINDOW_IN;
import static com.kurento.kms.thrift.api.KmsMediaPointerDetector2FilterTypeConstants.EVENT_WINDOW_OUT;
import static com.kurento.kms.thrift.api.KmsMediaPointerDetector2FilterTypeConstants.REMOVE_WINDOW;
import static com.kurento.kms.thrift.api.KmsMediaPointerDetector2FilterTypeConstants.REMOVE_WINDOW_PARAM_WINDOW_ID;
import static com.kurento.kms.thrift.api.KmsMediaPointerDetector2FilterTypeConstants.TRACK_COLOR_FROM_CALIBRATION_REGION;
import static com.kurento.kms.thrift.api.KmsMediaPointerDetector2FilterTypeConstants.TYPE_NAME;

import java.util.Map;

import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.ListenerRegistration;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PointerDetectorAdvFilter;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.events.WindowInEvent;
import com.kurento.kmf.media.events.WindowOutEvent;
import com.kurento.kmf.media.internal.refs.MediaElementRef;
import com.kurento.kmf.media.params.MediaParam;
import com.kurento.kmf.media.params.internal.PointerDetectorAdvConstructorParam;
import com.kurento.kmf.media.params.internal.PointerDetectorWindowMediaParam;
import com.kurento.kmf.media.params.internal.StringMediaParam;
import com.kurento.kmf.media.params.internal.WindowParam;

@ProvidesMediaElement(type = TYPE_NAME)
public class PointerDetectorAdvFilterImpl extends FilterImpl implements
		PointerDetectorAdvFilter {

	public PointerDetectorAdvFilterImpl(MediaElementRef filterId) {
		super(filterId);
	}

	/**
	 * @param objectRef
	 * @param params
	 */
	public PointerDetectorAdvFilterImpl(MediaElementRef objectRef,
			Map<String, MediaParam> params) {
		super(objectRef, params);
	}

	@Override
	public ListenerRegistration addWindowOutListener(
			MediaEventListener<WindowOutEvent> windowOutListener) {
		return addListener(EVENT_WINDOW_OUT, windowOutListener);
	}

	@Override
	public void addWindowOutListener(
			MediaEventListener<WindowOutEvent> windowOutListener,
			Continuation<ListenerRegistration> cont) {
		addListener(EVENT_WINDOW_OUT, windowOutListener, cont);
	}

	@Override
	public ListenerRegistration addWindowInListener(
			MediaEventListener<WindowInEvent> windowInListener) {
		return addListener(EVENT_WINDOW_IN, windowInListener);
	}

	@Override
	public void addWindowInListener(
			MediaEventListener<WindowInEvent> windowInListener,
			Continuation<ListenerRegistration> cont) {
		addListener(EVENT_WINDOW_IN, windowInListener, cont);
	}

	@Override
	public void removeWindow(String windowId) {
		Map<String, MediaParam> params = newHashMapWithExpectedSize(1);
		StringMediaParam param = new StringMediaParam();
		param.setString(windowId);
		params.put(REMOVE_WINDOW_PARAM_WINDOW_ID, param);
		invoke(REMOVE_WINDOW, params);
	}

	@Override
	public void clearWindows() {
		invoke(CLEAR_WINDOWS);
	}

	@Override
	public void trackcolourFromCalibrationRegion() {
		invoke(TRACK_COLOR_FROM_CALIBRATION_REGION);
	}

	@Override
	public void addWindow(PointerDetectorWindowMediaParam window) {
		Map<String, MediaParam> params = newHashMapWithExpectedSize(1);
		params.put(ADD_NEW_WINDOW_PARAM_WINDOW, window);
		invoke(ADD_NEW_WINDOW, params);
	}

	@Override
	public void addWindow(PointerDetectorWindowMediaParam window,
			Continuation<Void> cont) {
		Map<String, MediaParam> params = newHashMapWithExpectedSize(1);
		params.put(ADD_NEW_WINDOW_PARAM_WINDOW, window);
		invoke(ADD_NEW_WINDOW, params, new VoidContinuationWrapper(cont));
	}

	@Override
	public void removeWindow(String windowId, Continuation<Void> cont) {
		Map<String, MediaParam> params = newHashMapWithExpectedSize(1);
		StringMediaParam param = new StringMediaParam();
		param.setString(windowId);
		params.put(REMOVE_WINDOW_PARAM_WINDOW_ID, param);
		invoke(REMOVE_WINDOW, params, new VoidContinuationWrapper(cont));
	}

	@Override
	public void clearWindows(Continuation<Void> cont) {
		invoke(CLEAR_WINDOWS, new VoidContinuationWrapper(cont));
	}

	@Override
	public void trackColourFromCalibrationRegion(Continuation<Void> cont) {
		invoke(TRACK_COLOR_FROM_CALIBRATION_REGION,
				new VoidContinuationWrapper(cont));

	}

	public static class PointerDetectorAdvFilterBuilderImpl<T extends PointerDetectorAdvFilterBuilderImpl<T>>
			extends FilterBuilderImpl<T, PointerDetectorAdvFilter> implements
			PointerDetectorAdvFilterBuilder {

		private final PointerDetectorAdvConstructorParam param;

		public PointerDetectorAdvFilterBuilderImpl(
				final MediaPipeline pipeline,
				final WindowParam calibrationRegion) {
			super(TYPE_NAME, pipeline);
			param = new PointerDetectorAdvConstructorParam(calibrationRegion);
			params.put(CONSTRUCTOR_PARAMS_DATA_TYPE, param);
		}

		@Override
		public T withWindow(PointerDetectorWindowMediaParam window) {
			param.addDetectorWindow(window);
			return self();
		}

	}

}
