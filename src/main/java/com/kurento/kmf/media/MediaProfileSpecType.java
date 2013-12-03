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

import com.kurento.kms.thrift.api.KmsMediaMuxer;

/**
 * Enumeration of media profile types.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Ivan Gracia (igracia@gsyc.es)
 * @since 2.0.0
 */
public enum MediaProfileSpecType {
	WEBM(KmsMediaMuxer.WEBM), MP4(KmsMediaMuxer.MP4);

	private final KmsMediaMuxer specType;

	private MediaProfileSpecType(KmsMediaMuxer specType) {
		this.specType = specType;
	}

	/**
	 * Obtains the thrift equivalent type
	 * 
	 * @return The thrift type
	 */
	public KmsMediaMuxer toThrift() {
		return this.specType;
	}
}
