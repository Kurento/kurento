package com.kurento.kmf.spring;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KurentoApplicationContextConfiguration {

	@Bean
	protected RootWebApplicationContextParentRecoverer parentRecoverer() {
		return new RootWebApplicationContextParentRecoverer();
	}
}
