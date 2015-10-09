package org.kurento.test;

import static org.kurento.test.TestConfiguration.TEST_CONFIG_JSON_DEFAULT;

import org.kurento.commons.ConfigFileManager;

public class ConfigFileTest {

	static {
		ConfigFileManager.loadConfigFile(TEST_CONFIG_JSON_DEFAULT);
	}

}
