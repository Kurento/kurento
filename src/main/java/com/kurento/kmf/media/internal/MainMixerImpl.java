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

import com.kurento.kmf.media.MainMixer;
import com.kurento.kmf.media.MediaPipeline;
import com.kurento.kmf.media.internal.refs.MediaMixerRef;

// TODO the type should be obtained from the thrift interface
@ProvidesMediaElement(type = MainMixerImpl.TYPE)
public class MainMixerImpl extends MediaMixerImpl implements MainMixer {

	public static final String TYPE = "MainMixer";

	public MainMixerImpl(MediaMixerRef mainMixerId) {
		super(mainMixerId);
	}

	static class MainMixerBuilderImpl<T extends MainMixerBuilderImpl<T>>
			extends MediaMixerBuilderImpl<T, MainMixer> implements
			MainMixerBuilder {

		public MainMixerBuilderImpl(final MediaPipeline pipeline) {
			super(TYPE, pipeline);
		}

	}

}
