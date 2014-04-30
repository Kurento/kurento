package com.kurento.kmf.connector.test.base;

import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.context.WebApplicationContext;

import com.kurento.kmf.connector.ConnectorApp;
import com.kurento.kmf.spring.KurentoApplicationContextUtils;

public class BootBaseTest {

	protected static ConfigurableApplicationContext context;

	@BeforeClass
	public static void start() throws Exception {

		Properties properties = new Properties();
		properties.put("server.port", getPort());

		trasvaseProperty("kurento.serverAddress", "mediaserver.address",
				properties);
		trasvaseProperty("kurento.serverPort", "mediaserver.port", properties);
		trasvaseProperty("kurento.handlerAddress", "handler.address",
				properties);
		trasvaseProperty("kurento.handlerPort", "handler.port", properties);

		SpringApplication application = new SpringApplication(
				ConnectorApp.class);

		application.setDefaultProperties(properties);
		context = application.run();
	}

	@AfterClass
	public static void stop() {

		if(context != null) {
			context.close();
		}
	}

	private static void trasvaseProperty(String systemPropertyName,
			String propertyName, Properties properties) {
		String property = System.getProperty(systemPropertyName);

		if (property != null)
			properties.put(propertyName, property);

	}

	protected static String getPort() {
		String port = System.getProperty("http.port");
		if (port == null) {
			port = "7788";
		}
		return port;
	}

	public static void main(String[] args) throws Exception {
		start();
	}

}
