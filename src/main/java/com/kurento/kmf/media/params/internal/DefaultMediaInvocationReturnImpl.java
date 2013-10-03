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

public class DefaultMediaInvocationReturnImpl extends AbstractMediaParam {

	private byte[] result;

	public DefaultMediaInvocationReturnImpl(String dataType) {
		super(dataType);
	}

	public byte[] getResult() {
		return result;
	}

	/**
	 * This method deserializes a command result, storing the byte array
	 * contained in the data field from the result structure. If no data is set,
	 * the method return an empty array. </br> {@inheritDoc}
	 */
	@Override
	public void deserializeCommandResult(final KmsMediaParam commandResult) {
		this.result = (commandResult.isSetData()) ? Arrays.copyOf(
				commandResult.getData(), commandResult.getData().length)
				: new byte[0];
	}

	@Override
	protected byte[] getData() {
		// TODO Auto-generated method stub
		return null;
	}

}
