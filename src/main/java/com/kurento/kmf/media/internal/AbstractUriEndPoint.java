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

import static com.kurento.kms.thrift.api.KmsMediaUriEndPointTypeConstants.GET_URI;
import static com.kurento.kms.thrift.api.KmsMediaUriEndPointTypeConstants.PAUSE;
import static com.kurento.kms.thrift.api.KmsMediaUriEndPointTypeConstants.SET_URI;
import static com.kurento.kms.thrift.api.KmsMediaUriEndPointTypeConstants.SET_URI_PARAM_URI_STR;
import static com.kurento.kms.thrift.api.KmsMediaUriEndPointTypeConstants.START;
import static com.kurento.kms.thrift.api.KmsMediaUriEndPointTypeConstants.STOP;

import java.util.HashMap;
import java.util.Map;

import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.UriEndPoint;
import com.kurento.kmf.media.internal.refs.MediaElementRef;
import com.kurento.kmf.media.params.MediaParam;
import com.kurento.kmf.media.params.internal.StringMediaParam;

public abstract class AbstractUriEndPoint extends AbstractEndPoint implements
		UriEndPoint {

	public AbstractUriEndPoint(MediaElementRef endpointRef) {
		super(endpointRef);
	}

	/**
	 * @param objectRef
	 * @param params
	 */
	public AbstractUriEndPoint(MediaElementRef objectRef,
			Map<String, MediaParam> params) {
		super(objectRef, params);
	}

	/* SYNC */
	@Override
	public String getUri() {
		StringMediaParam result = (StringMediaParam) invoke(GET_URI);
		return result.getString();
	}

	@Override
	public void setUri(String uri) {
		Map<String, MediaParam> params = new HashMap<String, MediaParam>(4);
		StringMediaParam param = new StringMediaParam();
		param.setString(uri);
		params.put(SET_URI_PARAM_URI_STR, param);
		invoke(SET_URI, params);
	}

	void start() {
		invoke(START);
	}

	@Override
	public void pause() {
		invoke(PAUSE);
	}

	@Override
	public void stop() {
		invoke(STOP);
	}

	/* ASYNC */
	@Override
	public void getUri(final Continuation<String> cont) {
		invoke(GET_URI, new StringContinuationWrapper(cont));
	}

	@Override
	public void setUri(final String uri, final Continuation<Void> cont) {
		Map<String, MediaParam> params = new HashMap<String, MediaParam>(4);
		StringMediaParam param = new StringMediaParam();
		param.setString(uri);
		params.put(SET_URI_PARAM_URI_STR, param);
		invoke(SET_URI, new VoidContinuationWrapper(cont));
	}

	void start(final Continuation<Void> cont) {
		invoke(START, new VoidContinuationWrapper(cont));
	}

	@Override
	public void pause(final Continuation<Void> cont) {
		invoke(PAUSE, new VoidContinuationWrapper(cont));
	}

	@Override
	public void stop(final Continuation<Void> cont) {
		invoke(STOP, new VoidContinuationWrapper(cont));
	}
}
