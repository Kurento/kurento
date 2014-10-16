package org.kurento.tree.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.kurento.commons.PropertiesManager.getPropertyJson;

import java.util.List;

import org.junit.Test;
import org.kurento.commons.ConfigFileManager;
import org.kurento.commons.PropertiesManager;
import org.kurento.jsonrpc.JsonUtils;

import com.google.gson.JsonArray;

public class ConfigFileLoaderTest {

	@Test
	public void basicConfigFile() {

		ConfigFileManager.loadConfigFile("basic-kurento-tree.conf.json");

		assertThat(PropertiesManager.getProperty("ws.port"), is("8890"));
		assertThat(PropertiesManager.getProperty("ws.path"), is("kurento-tree"));

		JsonArray kmsUrisJson = getPropertyJson("kms.uris", null,
				JsonArray.class);
		List<String> kmsUris = JsonUtils.toStringList(kmsUrisJson);

		assertThat(kmsUris.get(0), is("ws://192.168.0.1:8888/kurento"));

	}

}
