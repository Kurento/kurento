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
package com.kurento.test.config;

import org.jboss.arquillian.junit.Arquillian;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import com.kurento.kmf.content.ContentApiConfiguration;
import com.kurento.kmf.spring.KurentoApplicationContextUtils;
import com.kurento.test.base.BaseArquillianTst;

/**
 * Integration test (JUnit/Arquillian) for custom configuration of KMF. It
 * contains two tests: i) custom configuration made by customizing
 * <code>kurento.properties</code> file; ii) custom configuration made by custom
 * spring bean.
 * 
 * @author Boni García (bgarcia@gsyc.es)
 * @since 1.0.0
 */
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
