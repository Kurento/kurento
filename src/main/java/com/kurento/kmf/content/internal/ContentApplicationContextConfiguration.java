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

import com.kurento.kmf.content.ContentApiConfiguration;
import com.kurento.kmf.content.PlayerHandler;
import com.kurento.kmf.content.RecorderHandler;
import com.kurento.kmf.content.RtpMediaHandler;
import com.kurento.kmf.content.WebRtcMediaHandler;
import com.kurento.kmf.content.internal.player.AsyncPlayerRequestProcessor;
import com.kurento.kmf.content.internal.player.PlayRequestImpl;
import com.kurento.kmf.content.internal.recorder.AsyncRecorderRequestProcessor;
import com.kurento.kmf.content.internal.recorder.RecordRequestImpl;
import com.kurento.kmf.content.internal.rtp.AsyncRtpMediaRequestProcessor;
import com.kurento.kmf.content.internal.rtp.RtpMediaRequestImpl;
import com.kurento.kmf.content.internal.webrtc.AsyncWebRtcMediaRequestProcessor;
import com.kurento.kmf.content.internal.webrtc.WebRtcMediaRequestImpl;
import com.kurento.kmf.content.jsonrpc.JsonRpcRequest;
import com.kurento.kmf.media.MediaApiConfiguration;
import com.kurento.kmf.spring.RootWebApplicationContextParentRecoverer;

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

	@Bean
	SecretGenerator secretGenerator() {
		return new SecretGenerator();
	}

	@Bean
	ControlProtocolManager controlPrototolManager() {
		return new ControlProtocolManager();
	}

	// PLAYER STUFF
	@Bean
	@Scope("prototype")
	PlayRequestImpl playRequestImpl(PlayerHandler playerHander,
			ContentRequestManager manager, AsyncContext ctx, String contentId,
			boolean redirect, boolean useControlProtocol) {
		return new PlayRequestImpl(playerHander, manager, ctx, contentId,
				redirect, useControlProtocol);
	}

	@Bean
	@Scope("prototype")
	AsyncPlayerRequestProcessor asyncPlayerRequestProcessor(
			PlayRequestImpl playRequest, JsonRpcRequest message,
			AsyncContext asyncCtx) {
		return new AsyncPlayerRequestProcessor(playRequest, message, asyncCtx);
	}

	// RECORDER STUFF

	@Bean
	@Scope("prototype")
	RecordRequestImpl recordRequestImpl(RecorderHandler recorderHander,
			ContentRequestManager manager, AsyncContext ctx, String contentId,
			boolean redirect, boolean useControlProtocol) {
		return new RecordRequestImpl(recorderHander, manager, ctx, contentId,
				redirect, useControlProtocol);
	}

	@Bean
	@Scope("prototype")
	AsyncRecorderRequestProcessor asyncRecorderRequestProcessor(
			PlayRequestImpl playRequest, JsonRpcRequest message,
			AsyncContext asyncCtx) {
		return new AsyncRecorderRequestProcessor(playRequest, message, asyncCtx);
	}

	// WEBRTC MEDIA STUFF
	@Bean
	@Scope("prototype")
	WebRtcMediaRequestImpl webRtcMediaRequestImpl(WebRtcMediaHandler handler,
			ContentRequestManager manager, AsyncContext asyncContext,
			String contentId) {
		return new WebRtcMediaRequestImpl(handler, manager, asyncContext,
				contentId);
	}

	@Bean
	@Scope("prototype")
	AsyncWebRtcMediaRequestProcessor asyncWebRtcMediaRequestProcessor(
			WebRtcMediaRequestImpl mediaRequest, JsonRpcRequest message,
			AsyncContext asyncCtx) {
		return new AsyncWebRtcMediaRequestProcessor(mediaRequest, message,
				asyncCtx);
	}

	// RTP MEDIA STUFF
	@Bean
	@Scope("prototype")
	RtpMediaRequestImpl rtpMediaRequestImpl(RtpMediaHandler handler,
			ContentRequestManager manager, AsyncContext asyncContext,
			String contentId) {
		return new RtpMediaRequestImpl(handler, manager, asyncContext,
				contentId);
	}

	@Bean
	@Scope("prototype")
	AsyncRtpMediaRequestProcessor asyncRtpMediaRequestProcessor(
			RtpMediaRequestImpl mediaRequest, JsonRpcRequest message,
			AsyncContext asyncCtx) {
		return new AsyncRtpMediaRequestProcessor(mediaRequest, message,
				asyncCtx);
	}

	// OTHER STUFF

	@Bean
	@Scope("prototype")
	ContentRequestManager contentRequestManager() {
		return new ContentRequestManager();
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
}
