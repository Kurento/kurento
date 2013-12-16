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
package com.kurento.kmf.media;

import com.google.common.collect.BiMap;
import com.google.common.collect.EnumBiMap;
import com.kurento.kms.thrift.api.KmsMediaType;

/**
 * Enumeration of types of media.
 * 
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.1
 */
public enum MediaType {

	AUDIO, DATA, VIDEO;

	private static final BiMap<MediaType, KmsMediaType> biMap = EnumBiMap
			.create(MediaType.class, KmsMediaType.class);

	static {
		biMap.put(AUDIO, KmsMediaType.AUDIO);
		biMap.put(DATA, KmsMediaType.DATA);
		biMap.put(VIDEO, KmsMediaType.VIDEO);
	}

	public KmsMediaType asKmsType() {
		return biMap.get(this);
	}

	public static MediaType fromKmsType(KmsMediaType kmsType) {
		return biMap.inverse().get(kmsType);
	}

}
