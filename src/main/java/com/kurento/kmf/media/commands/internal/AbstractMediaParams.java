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
package com.kurento.kmf.media.commands.internal;

import com.kurento.kmf.media.commands.MediaParams;
import com.kurento.kms.thrift.api.Params;

/**
 * @author Iván Gracia (igracia@gsyc.es)
 * 
 */
public abstract class AbstractMediaParams implements MediaParams {

	private final String dataType;

	private final byte[] data;

	protected AbstractMediaParams(String type, byte[] data) {
		this.dataType = type;
		this.data = new byte[data.length];
		System.arraycopy(data, 0, this.data, 0, data.length);
	}

	@Override
	public String getDataType() {
		return this.dataType;
	}

	public Params getThriftParams() {
		Params params = new Params();
		params.setDataType(this.getDataType());
		params.setData(this.getData());
		return params;
	}

}
