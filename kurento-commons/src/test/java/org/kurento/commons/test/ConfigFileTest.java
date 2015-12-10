package org.kurento.commons.test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;

import org.junit.Test;
import org.kurento.commons.ConfigFileFinder;
import org.kurento.commons.ConfigFilePropertyHolder;
import org.kurento.commons.PropertiesManager;

import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

public class ConfigFileTest {

	@Test
	public void testSimpleProperty() throws JsonSyntaxException,
			JsonIOException, IOException, URISyntaxException {

		Path configFilePath = ConfigFileFinder
				.searchConfigFileInDefaultPlaces("test.conf.json");

		ConfigFilePropertyHolder
				.configurePropertiesFromConfigFile(configFilePath);

		assertThat(PropertiesManager.getProperty("prop1"), is("value1"));
		assertThat(PropertiesManager.getProperty("prop2.prop1"), is("xxx"));
		assertThat(PropertiesManager.getProperty("nonExistingProp3"),
				is(nullValue()));
		assertThat(PropertiesManager.getProperty("nonExistingProp3.prop1"),
				is(nullValue()));

		System.setProperty("nonExistingProp4", "kkkk");

		assertThat(PropertiesManager.getProperty("nonExistingProp4"),
				is("kkkk"));

		System.setProperty("prop1", "kkkk");

		assertThat(PropertiesManager.getProperty("prop1"), is("kkkk"));

	}
}
