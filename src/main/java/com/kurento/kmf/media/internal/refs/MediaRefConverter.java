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
