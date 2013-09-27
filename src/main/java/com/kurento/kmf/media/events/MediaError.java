package com.kurento.kmf.media.events;

import static com.kurento.kmf.media.internal.refs.MediaRefConverter.fromThrift;

import com.kurento.kmf.media.internal.refs.MediaObjectRefDTO;
import com.kurento.kms.thrift.api.KmsError;

public final class MediaError {

	private final MediaObjectRefDTO objectRef;

	private final String description;

	private final Integer errorCode;

	private final String type;

	public MediaError(KmsError error) {
		this.type = error.type;
		this.description = error.description;
		this.errorCode = Integer.valueOf(error.errorCode);
		this.objectRef = fromThrift(error.source);
	}

	public MediaObjectRefDTO getObjectRef() {
		return this.objectRef;
	}

	public String getDescription() {
		return this.description;
	}

	public Integer getErrorCode() {
		return this.errorCode;
	}

	public String getType() {
		return this.type;
	}
}
