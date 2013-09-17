package com.kurento.kmf.media.internal.refs;

import com.kurento.kms.thrift.api.MediaObjectRef;

public abstract class MediaObjectRefDTO {

	protected final MediaObjectRef objectRef;

	/**
	 * This constructor is used to preserve immutability of
	 * {@link MediaObjectRefDTO#objectId}. A defensive copy of the id takes
	 * place, in order to have a unique reference.
	 * 
	 * @param id
	 */
	protected MediaObjectRefDTO(MediaObjectRef ref) {
		this.objectRef = ref.deepCopy();
	}

	public Long getId() {
		return this.objectRef.getId();
	}

	public String getToken() {
		return this.objectRef.getToken();
	}

	public MediaObjectRef getThriftRef() {
		return this.objectRef;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		if (this == obj) {
			return true;
		}

		if (!obj.getClass().equals(this.getClass())) {
			return false;
		}

		MediaObjectRefDTO moRef = (MediaObjectRefDTO) obj;
		return moRef.objectRef.id == this.objectRef.id;
	}

	@Override
	public int hashCode() {
		int result = 13;
		result = (result * 31 + (int) (this.objectRef.id ^ (this.objectRef.id >>> 32)));
		return result;
	}

}
