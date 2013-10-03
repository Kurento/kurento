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

import com.kurento.kms.thrift.api.KmsMediaParam;

/**
 * @author Ivan Gracia (igracia@gsyc.es)
 * 
 */
public class DefaultMediaParam extends AbstractMediaParam {

	/**
	 * @param type
	 */
	protected DefaultMediaParam(String type) {
		super("UNKNOWN PARAM TYPE");
	}

	@Override
	public byte[] getData() {
		// TODO provide impl
		return null;
	}

	@Override
	public void deserializeCommandResult(KmsMediaParam result) {
		// TODO provide impl
	}

}
