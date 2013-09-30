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

import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TMemoryBuffer;

public abstract class AbstractThriftSerializedCommand extends
		AbstractMediaCommand {

	protected AbstractThriftSerializedCommand(String type) {
		super(type);
	}

	@Override
	protected byte[] getData() {
		TMemoryBuffer tr = new TMemoryBuffer(64); // default size. Will grow if
													// necessary
		TProtocol pr = new TBinaryProtocol(tr);
		return getThriftSerializedData(pr);
	}

	protected abstract byte[] getThriftSerializedData(TProtocol pr);

}
