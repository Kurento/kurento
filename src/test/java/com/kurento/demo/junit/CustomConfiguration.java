package com.kurento.demo.junit;

import org.springframework.stereotype.Component;

import com.kurento.kmf.content.ContentApiConfiguration;

@Component
public class CustomConfiguration extends ContentApiConfiguration {

	@Override
	public int getProxyMaxConnectionsPerRoute() {
		return 200;
	}
}
