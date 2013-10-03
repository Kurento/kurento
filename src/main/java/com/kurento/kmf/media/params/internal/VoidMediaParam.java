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

import static com.kurento.kms.thrift.api.KmsMediaDataTypeConstants.VOID_DATA_TYPE;

import org.apache.thrift.protocol.TProtocol;

import com.kurento.kmf.media.internal.ProvidesMediaParam;
import com.kurento.kms.thrift.api.KmsMediaParam;

@ProvidesMediaParam(type = VOID_DATA_TYPE)
public class VoidMediaParam extends AbstractThriftSerializedMediaParam {

	VoidMediaParam() {
		super(VOID_DATA_TYPE);
	}

	@Override
	public void deserializeCommandResult(KmsMediaParam result) {
		// Nothing to do here
	}

	@Override
	protected void deserializeFromTProtocol(TProtocol pr) {
		// Nothing to do here
	}

	@Override
	protected TProtocol getThriftSerializedData(TProtocol pr) {
		// Nothing to do here
		return pr;
	}

}
