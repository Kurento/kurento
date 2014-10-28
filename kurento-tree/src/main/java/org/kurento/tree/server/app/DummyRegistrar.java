package org.kurento.tree.server.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DummyRegistrar implements KmsRegistrar {

	private static Logger log = LoggerFactory.getLogger(DummyRegistrar.class);

	@Override
	public void register(String wsUri) {
		log.info("Registered new KMS with wsUri:" + wsUri);
	}

}
