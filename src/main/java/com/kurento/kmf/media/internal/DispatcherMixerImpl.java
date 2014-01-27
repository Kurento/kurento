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
import static com.kurento.kms.thrift.api.KmsMediaDispatcherMixerTypeConstants.SET_MAIN_END_POINT;
import static com.kurento.kms.thrift.api.KmsMediaDispatcherMixerTypeConstants.SET_MAIN_END_POINT_PARAM_MIXER;
import static com.kurento.kms.thrift.api.KmsMediaDispatcherMixerTypeConstants.TYPE_NAME;
import static com.kurento.kms.thrift.api.KmsMediaDispatcherMixerTypeConstants.UNSET_MAIN_END_POINT;

import java.util.Map;

import com.kurento.kmf.media.Continuation;
import com.kurento.kmf.media.DispatcherMixer;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.MixerPort;
import com.kurento.kmf.media.internal.refs.MediaMixerRef;
import com.kurento.kmf.media.params.MediaParam;
import com.kurento.kmf.media.params.internal.MediaObjectRefParam;

@ProvidesMediaElement(type = TYPE_NAME)
public class DispatcherMixerImpl extends MediaMixerImpl implements
		DispatcherMixer {

	public DispatcherMixerImpl(MediaMixerRef mainMixerId) {
		super(mainMixerId);
	}

	public DispatcherMixerImpl(MediaMixerRef ref, Map<String, MediaParam> params) {
		super(ref, params);
	}

	@Override
	public void setMainEndPoint(MixerPort mixerEndpoint, Continuation<Void> cont) {
		Map<String, MediaParam> params = newHashMapWithExpectedSize(1);
		params.put(SET_MAIN_END_POINT_PARAM_MIXER, new MediaObjectRefParam(
				((AbstractMediaObject) mixerEndpoint).getObjectRef()));
		invoke(SET_MAIN_END_POINT, params, new VoidContinuationWrapper(cont));
	}

	@Override
	public void unsetMainEndPoint(Continuation<Void> cont) {
		invoke(UNSET_MAIN_END_POINT, new VoidContinuationWrapper(cont));
	}

	@Override
	public void setMainEndPoint(MixerPort mixerEndpoint) {
		Map<String, MediaParam> params = newHashMapWithExpectedSize(1);
		params.put(SET_MAIN_END_POINT_PARAM_MIXER, new MediaObjectRefParam(
				((AbstractMediaObject) mixerEndpoint).getObjectRef()));
		invoke(SET_MAIN_END_POINT, params);
	}

	@Override
	public void unsetMainEndPoint() {
		invoke(UNSET_MAIN_END_POINT);
	}

	static class DispatcherMixerBuilderImpl<T extends DispatcherMixerBuilderImpl<T>>
			extends AbstractMediaMixerBuilderImpl<T, DispatcherMixer> implements
			DispatcherMixerBuilder {

		public DispatcherMixerBuilderImpl(final MediaPipeline pipeline) {
			super(TYPE_NAME, pipeline);
		}

	}
}
