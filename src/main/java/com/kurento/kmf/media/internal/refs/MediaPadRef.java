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

import com.kurento.kmf.media.MediaType;
import com.kurento.kms.thrift.api.KmsMediaObjectRef;
import com.kurento.kms.thrift.api.KmsMediaObjectType;
import com.kurento.kms.thrift.api.KmsMediaPadDirection;

public class MediaPadRef extends MediaObjectRef {

	public MediaPadRef(KmsMediaObjectRef ref) {
		super(ref);
		KmsMediaObjectType objType = this.objectRef.getObjectType();
		if (!objType.isSetPad()) {
			throw new IllegalArgumentException(
					"The reference used does not contain an appropraite type MediaPadType");
		}
	}

	@Override
	public MediaPadRef deepCopy() {
		return new MediaPadRef(objectRef);
	}

	public MediaType getType() {
		KmsMediaObjectType objType = this.objectRef.getObjectType();
		return MediaType.fromKmsType(objType.getPad().mediaType);
	}

	public KmsMediaPadDirection getPadDirection() {
		KmsMediaObjectType objType = this.objectRef.getObjectType();
		return objType.getPad().direction;
	}

	public String getMediaDescription() {
		KmsMediaObjectType objType = this.objectRef.getObjectType();
		return objType.getPad().mediaDescription;
	}

}
