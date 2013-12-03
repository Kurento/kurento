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

import static com.kurento.kms.thrift.api.KmsMediaZBarFilterTypeConstants.EVENT_CODE_FOUND;
import static com.kurento.kms.thrift.api.KmsMediaZBarFilterTypeConstants.TYPE_NAME;

import java.util.Map;

import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.ListenerRegistration;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.ZBarFilter;
import com.kurento.kmf.media.events.CodeFoundEvent;
import com.kurento.kmf.media.events.MediaEventListener;
import com.kurento.kmf.media.internal.refs.MediaElementRef;
import com.kurento.kmf.media.params.MediaParam;

@ProvidesMediaElement(type = TYPE_NAME)
public class ZBarFilterImpl extends FilterImpl implements ZBarFilter {

	public ZBarFilterImpl(MediaElementRef filterId) {
		super(filterId);
	}

	/**
	 * @param objectRef
	 * @param params
	 */
	public ZBarFilterImpl(MediaElementRef objectRef,
			Map<String, MediaParam> params) {
		super(objectRef, params);
	}

	@Override
	public ListenerRegistration addCodeFoundDataListener(
			final MediaEventListener<CodeFoundEvent> listener) {
		return addListener(EVENT_CODE_FOUND, listener);
	}

	@Override
	public void addCodeFoundDataListener(
			final MediaEventListener<CodeFoundEvent> listener,
			final Continuation<ListenerRegistration> cont) {
		addListener(EVENT_CODE_FOUND, listener, cont);
	}

	public static class ZBarFilterBuilderImpl<T extends ZBarFilterBuilderImpl<T>>
			extends FilterBuilderImpl<T, ZBarFilter> implements
			ZBarFilterBuilder {

		public ZBarFilterBuilderImpl(final MediaPipeline pipeline) {
			super(TYPE_NAME, pipeline);
		}

	}

}
