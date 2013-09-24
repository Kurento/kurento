package com.kurento.kmf.media.internal;

import static com.kurento.kmf.media.internal.refs.MediaRefConverter.fromThrift;

import com.kurento.kmf.media.internal.refs.MediaObjectRefDTO;
import com.kurento.kms.thrift.api.Error;

public class MediaError {

	private MediaObjectRefDTO objectRef;

	private String description;

	private Integer errorCode;

	private String type;

	MediaError(Error error) {
		this.type = error.type;
		this.description = error.description;
		this.errorCode = error.errorCode;
		this.objectRef = fromThrift(error.source);
	}

	public MediaObjectRefDTO getObjectRef() {
		return this.objectRef;
	}

	public void setObjectRef(MediaObjectRefDTO objectRef) {
		this.objectRef = objectRef;
	}

	public String getDescription() {
		return this.description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Integer getErrorCode() {
		return this.errorCode;
	}

	public void setErrorCode(Integer errorCode) {
		this.errorCode = errorCode;
	}

	public String getType() {
		return this.type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
