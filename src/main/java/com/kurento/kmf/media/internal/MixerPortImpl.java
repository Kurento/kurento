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

import static com.kurento.kms.thrift.api.KmsMediaMixerPortTypeConstants.TYPE_NAME;

import java.util.Map;

import com.kurento.kmf.media.MixerPort;
import com.kurento.kmf.media.internal.refs.MediaElementRef;
import com.kurento.kmf.media.params.MediaParam;

/**
 * 
 * @author Ivan Gracia (izanmail@gmail.com)
 * @since 3.0.5
 * 
 */
// TODO add real type here
@ProvidesMediaElement(type = TYPE_NAME)
public class MixerPortImpl extends MediaElementImpl implements MixerPort {

	/**
	 * 
	 * 
	 * @param objectRef
	 *            element reference
	 */
	public MixerPortImpl(MediaElementRef objectRef) {
		super(objectRef);
	}

	/**
	 * 
	 * 
	 * @param objectRef
	 *            element reference
	 * @param params
	 *            map of parameters. The key is the name of the parameter, while
	 *            the value represents the param itself.
	 */
	public MixerPortImpl(MediaElementRef objectRef,
			Map<String, MediaParam> params) {
		super(objectRef, params);
	}

}
