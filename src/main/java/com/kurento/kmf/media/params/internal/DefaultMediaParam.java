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

import java.util.Arrays;

import com.kurento.kms.thrift.api.KmsMediaParam;

/**
 * @author Ivan Gracia (igracia@gsyc.es)
 * 
 */
public class DefaultMediaParam extends AbstractMediaParam {

	private byte[] data;

	/**
	 * @param type
	 */
	public DefaultMediaParam(String type) {
		super(type);
	}

	@Override
	public byte[] getData() {
		return this.data;
	}

	public void setData(byte[] data) {
		this.data = Arrays.copyOf(data, data.length);
	}

	/**
	 * This method deserializes a command result, storing the byte array
	 * contained in the data field from the result structure. If no data is set,
	 * the method return an empty array. </br> {@inheritDoc}
	 */
	@Override
	public void deserializeParam(final KmsMediaParam param) {
		this.data = (param.isSetData()) ? Arrays.copyOf(param.getData(),
				param.getData().length) : new byte[0];
	}

}
