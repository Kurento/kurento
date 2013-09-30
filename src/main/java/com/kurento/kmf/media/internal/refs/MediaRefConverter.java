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
package com.kurento.kmf.media.internal.refs;

import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kms.thrift.api.MediaObjectRef;
import com.kurento.kms.thrift.api.MediaObjectType;

public class MediaRefConverter {

	public static MediaObjectRefDTO fromThrift(MediaObjectRef in) {
		MediaObjectRefDTO out;
		MediaObjectType type = in.getType();

		if (type.isSetElementType()) {
			out = new MediaElementRefDTO(in);
		} else if (type.isSetMixerType()) {
			out = new MediaMixerRefDTO(in);
		} else if (type.isSetPadType()) {
			out = new MediaPadRefDTO(in);
		} else if (type.isSetPipelineType()) {
			out = new MediaPipelineRefDTO(in);
		} else {
			throw new KurentoMediaFrameworkException(
					"Unexpected object ref received from server");
		}

		return out;
	}

}
