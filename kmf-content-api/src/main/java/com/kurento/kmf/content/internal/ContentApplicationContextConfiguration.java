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
package com.kurento.kmf.content.internal;

import javax.servlet.AsyncContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;

import com.kurento.kmf.common.SecretGenerator;
import com.kurento.kmf.content.ContentApiConfiguration;
import com.kurento.kmf.content.HttpPlayerHandler;
import com.kurento.kmf.content.HttpRecorderHandler;
import com.kurento.kmf.content.RtpContentHandler;
import com.kurento.kmf.content.WebRtcContentHandler;
import com.kurento.kmf.content.internal.base.AbstractContentSession;
import com.kurento.kmf.content.internal.base.AsyncContentRequestProcessor;
import com.kurento.kmf.content.internal.player.HttpPlayerSessionImpl;
import com.kurento.kmf.content.internal.recorder.HttpRecorderSessionImpl;
import com.kurento.kmf.content.internal.rtp.RtpContentSessionImpl;
import com.kurento.kmf.content.internal.webrtc.WebRtcContentSessionImpl;
import com.kurento.kmf.content.jsonrpc.JsonRpcRequest;
import com.kurento.kmf.media.MediaApiConfiguration;
import com.kurento.kmf.spring.RootWebApplicationContextParentRecoverer;
import com.kurento.kmf.thrift.ThriftInterfaceConfiguration;

/**
 * 
 * Configuration class, declaring the Spring beans used in Content Management
 * API.
 * 
 * @author Luis López (llopez@gsyc.es)
 * @author Boni García (bgarcia@gsyc.es)
 * @version 1.0.0
 */
@Configuration
public class ContentApplicationContextConfiguration {

	private static final Logger log = LoggerFactory
			.getLogger(ContentApplicationContextConfiguration.class);

	@Autowired
	private RootWebApplicationContextParentRecoverer parentRecoverer;

	@Bean
	StreamingProxy streamingProxy() {
		return new StreamingProxy();
	}

	@Bean
	ContentApiExecutorService contentApiExecutorService() {
		return new ContentApiExecutorService();
	}

	/**
	 * Random word generator.
	 * 
	 * @return Random word generator bean
	 */
	@Bean
	SecretGenerator secretGenerator() {
		return new SecretGenerator();
	}

	/**
	 * Protocol manager.
	 * 
	 * @return Protocol manager bean
	 */
	@Bean
	ControlProtocolManager controlPrototolManager() {
		return new ControlProtocolManager();
	}

	@Bean
	@Scope("prototype")
	AsyncContentRequestProcessor asyncContentRequestProcessor(
			AbstractContentSession contentSession, JsonRpcRequest message,
			AsyncContext asyncCtx) {
		return new AsyncContentRequestProcessor(contentSession, message,
				asyncCtx);
	}

	// PLAYER STUFF
	@Bean
	@Scope("prototype")
	HttpPlayerSessionImpl httpPlayerSessionImpl(
			HttpPlayerHandler playerHandler, ContentSessionManager manager,
			AsyncContext ctx, String contentId, boolean redirect,
			boolean useControlProtocol) {
		return new HttpPlayerSessionImpl(playerHandler, manager, ctx,
				contentId, redirect, useControlProtocol);
	}

	// RECORDER STUFF
	@Bean
	@Scope("prototype")
	HttpRecorderSessionImpl httpRecordSessionImpl(
			HttpRecorderHandler recorderHander, ContentSessionManager manager,
			AsyncContext ctx, String contentId, boolean redirect,
			boolean useControlProtocol) {
		return new HttpRecorderSessionImpl(recorderHander, manager, ctx,
				contentId, redirect, useControlProtocol);
	}

	// WEBRTC MEDIA STUFF
	@Bean
	@Scope("prototype")
	WebRtcContentSessionImpl webRtcContentSessionImpl(
			WebRtcContentHandler handler, ContentSessionManager manager,
			AsyncContext asyncContext, String contentId) {
		return new WebRtcContentSessionImpl(handler, manager, asyncContext,
				contentId);
	}

	// RTP MEDIA STUFF
	@Bean
	@Scope("prototype")
	RtpContentSessionImpl rtpContentSessionImpl(RtpContentHandler handler,
			ContentSessionManager manager, AsyncContext asyncContext,
			String contentId) {
		return new RtpContentSessionImpl(handler, manager, asyncContext,
				contentId);
	}

	// OTHER STUFF
	@Bean
	@Scope("prototype")
	ContentSessionManager contentSessionManager() {
		return new ContentSessionManager();
	}

	@Bean
	@Primary
	ContentApiConfiguration contentApiConfiguration() {
		try {
			return parentRecoverer.getParentContext().getBean(
					ContentApiConfiguration.class);
		} catch (NullPointerException npe) {
			log.info("Configuring Content API. Could not find parent context. Switching to default configuration ...");
		} catch (NoSuchBeanDefinitionException t) {
			log.info("Configuring Content API. Could not find exacly one bean of class "
					+ ContentApiConfiguration.class.getSimpleName()
					+ ". Switching to default configuration ...");
		}
		return new ContentApiConfiguration();
	}

	@Bean
	@Primary
	MediaApiConfiguration mediaApiConfiguration() {
		try {
			return parentRecoverer.getParentContext().getBean(
					MediaApiConfiguration.class);
		} catch (NullPointerException npe) {
			log.info("Configuring Media API. Could not find parent context. Switching to default configuration ...");
		} catch (NoSuchBeanDefinitionException t) {
			log.info("Configuring Media API. Could not find exacly one bean of class "
					+ MediaApiConfiguration.class.getSimpleName()
					+ ". Switching to default configuration ...");
		}
		return new MediaApiConfiguration();
	}

	@Bean
	@Primary
	ThriftInterfaceConfiguration thriftInterfaceConfiguration() {
		try {
			return parentRecoverer.getParentContext().getBean(
					ThriftInterfaceConfiguration.class);
		} catch (NullPointerException npe) {
			log.info("Configuring Media API. Could not find parent context. Switching to default configuration ...");
		} catch (NoSuchBeanDefinitionException t) {
			log.info("Configuring Media API. Could not find exacly one bean of class "
					+ ThriftInterfaceConfiguration.class.getSimpleName()
					+ ". Switching to default configuration ...");
		}
		return new ThriftInterfaceConfiguration();
	}
}
