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

import com.kurento.kmf.media.commands.MediaCommand;
import com.kurento.kmf.media.commands.MediaParams;
import com.kurento.kms.thrift.api.Command;
import com.kurento.kms.thrift.api.Params;

public abstract class AbstractMediaCommand implements MediaCommand {

	private final String name;

	private final MediaParams mediaParams;

	protected AbstractMediaCommand(String name, MediaParams params) {
		this.name = name;
		this.mediaParams = params;// TODO maybe it´s better to create the params
									// in here.
	}

	@Override
	public String getName() {
		return this.name;
	}

	@Override
	public MediaParams getParams() {
		return this.getParams();
	}

	protected abstract byte[] getData();

	public Command getThriftCommand() {
		Command command = new Command();
		command.params = new Params();
		command.params.setDataType(mediaParams.getDataType());
		command.params.setData(getData());
		return command;
	}

}
