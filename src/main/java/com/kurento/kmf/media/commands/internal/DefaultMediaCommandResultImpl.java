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

import java.util.Arrays;

import com.kurento.kms.thrift.api.Params;

public class DefaultMediaCommandResultImpl extends AbstractMediaCommandResult {

	private byte[] result;

	public DefaultMediaCommandResultImpl() {
		super();
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
	public void deserializeCommandResult(final Params result) {
		this.result = (result.isSetData()) ? Arrays.copyOf(result.getData(),
				result.getData().length) : new byte[0];
	}

}
