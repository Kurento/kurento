package com.kurento.demo.junit;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.kurento.kmf.content.ContentApiConfiguration;
import com.kurento.kmf.spring.KurentoApplicationContextUtils;

@RunWith(Arquillian.class)
public class CustomConfigurationIT extends BaseArquillianTst {

	@Test
	public void testCustomConfigByProperties() {
		ContentApiConfiguration configuration = KurentoApplicationContextUtils
				.getConfiguration(ContentApiConfiguration.class);

		Assert.assertEquals(20, configuration.getPoolCoreSize());
	}

	@Test
	public void testCustomConfigBySpring() {
		ContentApiConfiguration configuration = KurentoApplicationContextUtils
				.getConfiguration(ContentApiConfiguration.class);

		Assert.assertEquals(200, configuration.getProxyMaxConnectionsPerRoute());
	}
}
