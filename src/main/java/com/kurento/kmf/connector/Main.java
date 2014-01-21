package com.kurento.kmf.connector;

import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

public class Main {

	protected static ConfigurableApplicationContext context;

	protected static String getPort() {
		String port = System.getProperty("http.port");
		if (port == null) {
			port = "7788";
		}
		return port;
	}
	
	public static void main(String[] args) throws Exception {
		
		Properties properties = new Properties();
		properties.put("server.port", getPort());
		properties.put("debug", "");
				
		SpringApplication application = new SpringApplication(
				BootApplication.class);
		
		application.setDefaultProperties(properties);
		
		context = application.run();
		
	}
	
}
