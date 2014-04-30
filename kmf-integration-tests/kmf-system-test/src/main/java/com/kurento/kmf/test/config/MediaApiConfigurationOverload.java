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
package com.kurento.kmf.test.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Strings;
import com.kurento.kmf.media.MediaApiConfiguration;
import com.kurento.kmf.test.PropertiesManager;

public class MediaApiConfigurationOverload extends MediaApiConfiguration {

	public static final Logger log = LoggerFactory
			.getLogger(MediaApiConfigurationOverload.class);

	private String handlerAddress;

	private Integer handlerPort;

	/**
	 * Gets the address of the local thrift server, which will be used to
	 * receive events and error notifications sent by the Kurento Media Server.
	 * 
	 * @return The handler address.
	 */
	@Override
	public String getHandlerAddress() {

		if (Strings.isNullOrEmpty(handlerAddress)) {
			handlerAddress = PropertiesManager.getSystemProperty(
					"kurento.handlerAddress", "127.0.0.1");
			log.info("[Bean Override] handlerAddress configured {}",
					handlerAddress);
		}

		return handlerAddress;
	}

	/**
	 * Gets the port of the local thrift server, which will be used to receive
	 * events and error notifications sent by the Kurento Media Server.
	 * 
	 * @return The local thrift server port.
	 */
	@Override
	public int getHandlerPort() {

		if (handlerPort == null) {
			handlerPort = Integer.valueOf(PropertiesManager.getSystemProperty(
					"kurento.handlerPort", 9104));
			log.info("[Bean Override] handlerPort configured {}", handlerPort);
		}
		return handlerPort.intValue();
	}
}
