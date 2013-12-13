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

import static com.kurento.kms.thrift.api.KmsMediaPointerDetectorFilterTypeConstants.CLEAR_WINDOWS;
import static com.kurento.kms.thrift.api.KmsMediaPointerDetectorFilterTypeConstants.CONSTRUCTOR_PARAMS_DATA_TYPE;
import static com.kurento.kms.thrift.api.KmsMediaPointerDetectorFilterTypeConstants.EVENT_WINDOW_IN;
import static com.kurento.kms.thrift.api.KmsMediaPointerDetectorFilterTypeConstants.EVENT_WINDOW_OUT;
import static com.kurento.kms.thrift.api.KmsMediaPointerDetectorFilterTypeConstants.REMOVE_WINDOW;
import static com.kurento.kms.thrift.api.KmsMediaPointerDetectorFilterTypeConstants.REMOVE_WINDOW_PARAM_WINDOW_ID;
import static com.kurento.kms.thrift.api.KmsMediaPointerDetectorFilterTypeConstants.TYPE_NAME;

import java.util.HashMap;
import java.util.Map;

import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.ListenerRegistration;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.PointerDetectorFilter;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.events.WindowInEvent;
import com.kurento.kmf.media.events.WindowOutEvent;
import com.kurento.kmf.media.internal.refs.MediaElementRef;
import com.kurento.kmf.media.params.MediaParam;
import com.kurento.kmf.media.params.internal.PointerDetectorConstructorParam;
import com.kurento.kmf.media.params.internal.PointerDetectorWindowMediaParam;
import com.kurento.kmf.media.params.internal.StringMediaParam;

@ProvidesMediaElement(type = TYPE_NAME)
public class PointerDetectorFilterImpl extends FilterImpl implements
		PointerDetectorFilter {

	public PointerDetectorFilterImpl(MediaElementRef filterId) {
		super(filterId);
	}

	/**
	 * @param objectRef
	 * @param params
	 */
	public PointerDetectorFilterImpl(MediaElementRef objectRef,
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
		Map<String, MediaParam> params = new HashMap<String, MediaParam>(4);
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
	public void removeWindow(String windowId, Continuation<Void> cont) {
		Map<String, MediaParam> params = new HashMap<String, MediaParam>(4);
		StringMediaParam param = new StringMediaParam();
		param.setString(windowId);
		params.put(REMOVE_WINDOW_PARAM_WINDOW_ID, param);
		invoke(REMOVE_WINDOW, params);
	}

	@Override
	public void clearWindows(Continuation<Void> cont) {
		invoke(CLEAR_WINDOWS, new VoidContinuationWrapper(cont));
	}

	public static class PointerDetectorFilterBuilderImpl<T extends PointerDetectorFilterBuilderImpl<T>>
			extends FilterBuilderImpl<T, PointerDetectorFilter> implements
			PointerDetectorFilterBuilder {

		private PointerDetectorConstructorParam param;

		public PointerDetectorFilterBuilderImpl(final MediaPipeline pipeline) {
			super(TYPE_NAME, pipeline);
		}

		@Override
		public T withWindow(PointerDetectorWindowMediaParam window) {
			param.addDetectorWindow(window);
			return self();
		}

		private PointerDetectorConstructorParam initialiseMediaParam() {
			if (param == null) {
				param = new PointerDetectorConstructorParam();
				params.put(CONSTRUCTOR_PARAMS_DATA_TYPE, param);
			}
			return param;
		}

	}

	@Override
	public void addWindow(PointerDetectorWindowMediaParam window) {
		// Map<String, MediaParam> params = new HashMap<String, MediaParam>(4);
		// PointerDetectorWindowMediaParam param = new
		// PointerDetectorWindowMediaParam(
		// id, height, width, upperRightX, upperRightY);
		// params.put(ADD_NEW_WINDOW_PARAM_WINDOW, param);
		// invoke(ADD_NEW_WINDOW, params);
	}

	@Override
	public void addWindow(PointerDetectorWindowMediaParam window,
			Continuation<Void> cont) {
		// Map<String, MediaParam> params = new HashMap<String, MediaParam>(4);
		// PointerDetectorWindowMediaParam param = new
		// PointerDetectorWindowMediaParam(
		// id, height, width, upperRightX, upperRightY);
		// params.put(ADD_NEW_WINDOW_PARAM_WINDOW, param);
		// invoke(ADD_NEW_WINDOW, params);
	}

}
