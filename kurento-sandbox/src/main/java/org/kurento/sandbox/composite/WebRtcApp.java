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
package org.kurento.sandbox.composite;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Scope;
import org.springframework.core.env.Environment;
import org.kurento.client.factory.KurentoClientFactory;
import org.kurento.client.factory.KurentoClient;
import org.kurento.jsonrpc.internal.server.config.JsonRpcConfiguration;
import org.kurento.jsonrpc.server.JsonRpcConfigurer;
import org.kurento.jsonrpc.server.JsonRpcHandlerRegistry;

/**
 * Composite WebRTC demo (main).
 * 
 * @author Boni Garcia (bgarcia@gsyc.es)
 * @author David Fernández-López (d.fernandezlop@gmail.com)
 * @since 4.2.3
 */
@Configuration
@EnableAutoConfiguration
@Import(JsonRpcConfiguration.class)
public class WebRtcApp implements JsonRpcConfigurer {

	@Autowired
	private Environment env;

	@Override
	public void registerJsonRpcHandlers(JsonRpcHandlerRegistry registry) {
		registry.addPerSessionHandler(WebRtcHandler.class, "/webrtc");
	}

	@Bean
	KurentoClient mediaPipelineFactory() {
		return KurentoClientFactory.createKurentoClient();
	}

	@Bean
	public Room room() {
		return new Room();
	}

	@Bean
	@Scope("prototype")
	public WebRtcHandler multipleJsonRpcHandler() {
		return new WebRtcHandler();
	}

	public static void main(String[] args) throws Exception {
		SpringApplication application = new SpringApplication(WebRtcApp.class);
		application.run(args);
	}
}
