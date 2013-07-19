package com.kurento.kmf.content.servlet;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ContentApplicationContextConfiguration {

	@Bean
	HandlerServletAsyncExecutor handlerServletAsyncExecutor(){
		return new HandlerServletAsyncExecutor();
	}
}
