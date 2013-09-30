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
