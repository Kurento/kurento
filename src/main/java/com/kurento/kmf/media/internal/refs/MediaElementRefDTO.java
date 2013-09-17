package com.kurento.kmf.media.internal.refs;

import com.kurento.kms.thrift.api.MediaObjectRef;
import com.kurento.kms.thrift.api.MediaObjectType;

public class MediaElementRefDTO extends MediaObjectRefDTO {

	public MediaElementRefDTO(MediaObjectRef ref) {
		super(ref);
		MediaObjectType objType = this.objectRef.getType();
		if (!objType.isSetElementType()) {
			throw new IllegalArgumentException(
					"The reference used does not contain an appropraite type MediaElementType");
		}
	}

	public String getType() {
		MediaObjectType objType = this.objectRef.getType();
		return objType.getElementType().elementType;
	}

}
