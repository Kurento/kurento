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

import java.net.InetSocketAddress;

import javax.servlet.AsyncContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;

import com.kurento.kmf.common.SecretGenerator;
import com.kurento.kmf.common.exception.KurentoException;
import com.kurento.kmf.content.*;
import com.kurento.kmf.content.internal.base.AbstractContentSession;
import com.kurento.kmf.content.internal.base.AsyncContentRequestProcessor;
import com.kurento.kmf.content.internal.player.HttpPlayerSessionImpl;
import com.kurento.kmf.content.internal.recorder.HttpRecorderSessionImpl;
import com.kurento.kmf.content.internal.rtp.RtpContentSessionImpl;
import com.kurento.kmf.content.internal.webrtc.WebRtcContentSessionImpl;
import com.kurento.kmf.content.jsonrpc.JsonRpcRequest;
import com.kurento.kmf.jsonrpcconnector.client.JsonRpcClient;
import com.kurento.kmf.media.MediaApiConfiguration;
import com.kurento.kmf.media.factory.MediaPipelineFactory;
import com.kurento.kmf.spring.RootWebApplicationContextParentRecoverer;
import com.kurento.kmf.thrift.ThriftInterfaceConfiguration;
import com.kurento.kmf.thrift.internal.ThriftInterfaceExecutorService;
import com.kurento.kmf.thrift.jsonrpcconnector.JsonRpcClientThrift;
import com.kurento.kmf.thrift.pool.ThriftClientPoolService;

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

	private <E> E getBeanInParentOrDefault(Class<E> beanClass) {

		E bean = returnBeanInParent(beanClass);

		if (bean != null) {
			return bean;
		} else {
			try {
				return beanClass.newInstance();
			} catch (InstantiationException | IllegalAccessException e) {
				throw new KurentoException(
						"Exception creating default configuration for "
								+ beanClass.getSimpleName(), e);
			}
		}
	}

	private <E> E returnBeanInParent(Class<E> beanClass) {
		try {
			return parentRecoverer.getParentContext().getBean(beanClass);
		} catch (NullPointerException npe) {
			log.info(
					"Loading {}. Could not find parent context. Switching to default configuration ...",
					beanClass.getSimpleName());
			return null;
		} catch (NoSuchBeanDefinitionException t) {
			log.info("Loading {}. Could not find exacly one bean of class "
					+ beanClass.getSimpleName()
					+ ". Switching to default configuration ...");
			return null;
		}
	}

	@Bean
	@Primary
	ContentApiConfiguration contentApiConfiguration() {
		return getBeanInParentOrDefault(ContentApiConfiguration.class);
	}

	@Bean
	@Primary
	public MediaPipelineFactory mediaPipelineFactory() {

		MediaPipelineFactory bean = returnBeanInParent(MediaPipelineFactory.class);

		if (bean != null) {
			return bean;

		} else {

			ThriftClientPoolService clientPool = new ThriftClientPoolService(
					thriftInterfaceConfiguration());

			ThriftInterfaceExecutorService executorService = new ThriftInterfaceExecutorService(
					thriftInterfaceConfiguration());

			MediaApiConfiguration mediaApiConfiguration = mediaApiConfiguration();

			JsonRpcClient client = new JsonRpcClientThrift(clientPool,
					executorService, new InetSocketAddress(
							mediaApiConfiguration.getHandlerAddress(),
							mediaApiConfiguration.getHandlerPort()));

			return new MediaPipelineFactory(client);
		}
	}

	@Bean
	@Primary
	public ThriftInterfaceConfiguration thriftInterfaceConfiguration() {
		return getBeanInParentOrDefault(ThriftInterfaceConfiguration.class);
	}

	@Bean
	@Primary
	MediaApiConfiguration mediaApiConfiguration() {
		return getBeanInParentOrDefault(MediaApiConfiguration.class);
	}
}
