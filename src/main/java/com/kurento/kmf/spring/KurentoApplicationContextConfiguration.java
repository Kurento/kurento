package com.kurento.kmf.spring;

import org.springframework.beans.factory.config.PropertyOverrideConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class KurentoApplicationContextConfiguration {

	@Bean
	public PropertyOverrideConfigurer propertyOverrideConfigurer() {
		PropertyOverrideConfigurer poc = new PropertyOverrideConfigurer();
		poc.setFileEncoding("UTF-8");
		poc.setIgnoreResourceNotFound(true);
		poc.setLocation(new ClassPathResource("/WEB-INF/kurento.properties"));
		return poc;
	}

	@Bean
	protected RootWebApplicationContextParentRecoverer parentRecoverer() {
		return new RootWebApplicationContextParentRecoverer();
	}
}
