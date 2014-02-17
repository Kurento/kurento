/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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

import static com.kurento.kms.thrift.api.KmsMediaCompositeMixerTypeConstants.TYPE_NAME;

import java.util.Map;

import com.kurento.kmf.media.CompositeMixer;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.internal.refs.MediaMixerRef;
import com.kurento.kmf.media.params.MediaParam;

@ProvidesMediaElement(type = TYPE_NAME)
public class CompositeMixerImpl extends MediaMixerImpl implements
		CompositeMixer {

	public CompositeMixerImpl(MediaMixerRef mainMixerId) {
		super(mainMixerId);
	}

	public CompositeMixerImpl(MediaMixerRef ref, Map<String, MediaParam> params) {
		super(ref, params);
	}

	static class CompositeMixerBuilderImpl<T extends CompositeMixerBuilderImpl<T>>
			extends AbstractMediaMixerBuilderImpl<T, CompositeMixer> implements
			CompositeMixerBuilder {

		public CompositeMixerBuilderImpl(final MediaPipeline pipeline) {
			super(TYPE_NAME, pipeline);
		}

	}
}