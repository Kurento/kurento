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
package com.kurento.kmf.media.params.internal;

import com.kurento.kmf.media.params.MediaParam;
import com.kurento.kms.thrift.api.KmsMediaParam;

public abstract class AbstractMediaParam implements MediaParam {

	protected String dataType;

	/**
	 * @param type
	 */
	protected AbstractMediaParam(String type) {
		this.dataType = type;
	}

	@Override
	public String getDataType() {
		return this.dataType;
	}

	protected abstract byte[] getData();

	/**
	 * Deserializes a param obtained form the media server. Implementations of
	 * this class are responsible for the correct deserialization, since each
	 * param has different payload.
	 * 
	 * @param param
	 *            The param as a thrift
	 *            {@link com.kurento.kms.thrift.api.KmsMediaParam} structure
	 */
	public abstract void deserializeParam(KmsMediaParam param);

	public KmsMediaParam getThriftParams() {
		KmsMediaParam params = new KmsMediaParam();
		params.setDataType(this.dataType);
		params.setData(this.getData());
		return params;
	}

}
