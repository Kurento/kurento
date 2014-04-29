/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
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
package com.kurento.kmf.orion.internal;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;

import com.kurento.kmf.orion.OrionConnector;
import com.kurento.kmf.orion.OrionConnectorConfiguration;

/**
 * Context configuration for Spring
 * 
 * @author Ivan Gracia (izanmail@gmail.com)
 * 
 */
@Configuration
public class OrionConnectorApplicationContextConfiguration {

	@Bean
	OrionConnectorConfiguration orionConnectorConfiguration() {
		return new OrionConnectorConfiguration();
	}

	@Bean
	@Scope("prototype")
	OrionConnector orionConnector() {
		return new OrionConnector(orionConnectorConfiguration());
	}

}
