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
import com.kurento.kms.thrift.api.Command;

public abstract class AbstractMediaCommand implements MediaCommand {

	private final String type;

	protected AbstractMediaCommand(String type) {
		this.type = type;
	}

	@Override
	public String getType() {
		return this.type;
	}

	protected abstract byte[] getData();

	public Command getThriftCommand() {
		Command command = new Command();
		command.setType(getType());
		command.setData(getData());
		return command;
	}

}
