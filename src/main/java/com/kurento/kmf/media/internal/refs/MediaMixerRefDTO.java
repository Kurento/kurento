package com.kurento.kmf.media.internal.refs;

import com.kurento.kms.thrift.api.MediaObjectRef;
import com.kurento.kms.thrift.api.MediaObjectType;

public class MediaMixerRefDTO extends MediaObjectRefDTO {

	public MediaMixerRefDTO(MediaObjectRef ref) {
		super(ref);
		MediaObjectType objType = this.objectRef.getType();
		if (!objType.isSetMixerType()) {
			throw new IllegalArgumentException(
					"The reference used does not contain an appropraite type MediaMixerType");
		}
	}

	public String getType() {
		MediaObjectType objType = this.objectRef.getType();
		return objType.getMixerType().mixerType;
	}

}
