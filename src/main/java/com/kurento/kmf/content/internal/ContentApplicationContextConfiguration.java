package com.kurento.kmf.content.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.kurento.kmf.spring.RootWebApplicationContextParentRecoverer;

@Configuration
public class ContentApplicationContextConfiguration {

	private static final Logger log = LoggerFactory
			.getLogger(ContentApplicationContextConfiguration.class);

	@Autowired
	private RootWebApplicationContextParentRecoverer parentRecoverer;

	@Bean
	HandlerServletAsyncExecutor handlerServletAsyncExecutor() {
		return new HandlerServletAsyncExecutor();
	}

	@Bean
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
