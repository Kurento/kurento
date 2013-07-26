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
import com.kurento.kmf.content.PlayRequest;
import com.kurento.kmf.content.RecordRequest;
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
	HandlerServletAsyncExecutor handlerServletAsyncExecutor() {
		return new HandlerServletAsyncExecutor();
	}

	@Bean
	@Scope("prototype")
	PlayRequest playRequest(AsyncContext ctx, String contentId, boolean redirect) {
		return new PlayRequestImpl(ctx, contentId, redirect);
	}

	@Bean
	@Scope("prototype")
	RecordRequest recordRequest(AsyncContext ctx, String contentId,
			boolean redirect) {
		return new RecordRequestImpl(ctx, contentId, redirect);
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
}
