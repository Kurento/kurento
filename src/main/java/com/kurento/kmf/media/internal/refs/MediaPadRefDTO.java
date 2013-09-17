package com.kurento.kmf.media.internal.refs;

import com.kurento.kms.thrift.api.MediaObjectRef;
import com.kurento.kms.thrift.api.MediaObjectType;
import com.kurento.kms.thrift.api.MediaType;
import com.kurento.kms.thrift.api.PadDirection;

public class MediaPadRefDTO extends MediaObjectRefDTO {

	public MediaPadRefDTO(MediaObjectRef ref) {
		super(ref);
		MediaObjectType objType = this.objectRef.getType();
		if (!objType.isSetElementType()) {
			throw new IllegalArgumentException(
					"The reference used does not contain an appropraite type MediaPadType");
		}
	}

	public MediaType getType() {
		MediaObjectType objType = this.objectRef.getType();
		return objType.getPadType().mediaType;
	}

	public PadDirection getPadDirection() {
		MediaObjectType objType = this.objectRef.getType();
		return objType.getPadType().direction;
	}

	public String getMediaDescription() {
		MediaObjectType objType = this.objectRef.getType();
		return objType.getPadType().mediaDescription;
	}

}
