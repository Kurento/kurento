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
package com.kurento.kmf.connector;

public class ThriftConnectorConfiguration {

	/**
	 * Address of the local thrift server, which will be used to receive events
	 * and error notifications sent by the Kurento Media Server.
	 */
	private String handlerAddress = "localhost";

	/**
	 * Port where the local thrift server will be listening.
	 */
	private int handlerPort = 5555;

	/**
	 * Gets the address of the local thrift server, which will be used to
	 * receive events and error notifications sent by the Kurento Media Server.
	 * 
	 * @return The handler address.
	 */
	public String getHandlerAddress() {
		return handlerAddress;
	}

	/**
	 * Sets the address of the local thrift server, which will be used to
	 * receive events and error notifications sent by the Kurento Media Server.
	 * 
	 * @param handlerAddress
	 *            The address.
	 */
	public void setHandlerAddress(String handlerAddress) {
		this.handlerAddress = handlerAddress;
	}

	/**
	 * Gets the port of the local thrift server, which will be used to receive
	 * events and error notifications sent by the Kurento Media Server.
	 * 
	 * @return The local thrift server port.
	 */
	public int getHandlerPort() {
		return handlerPort;
	}

	/**
	 * Sets the port of the local thrift server, which will be used to receive
	 * events and error notifications sent by the Kurento Media Server.
	 * 
	 * @param handlerPort
	 *            The local thrift server port.
	 */
	public void setHandlerPort(int handlerPort) {
		this.handlerPort = handlerPort;
	}

}
