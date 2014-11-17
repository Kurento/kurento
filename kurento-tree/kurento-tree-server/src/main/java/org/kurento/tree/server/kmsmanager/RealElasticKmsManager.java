package org.kurento.tree.server.kmsmanager;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.kurento.client.KurentoClient;
import org.kurento.tree.server.app.KmsRegistrar;
import org.kurento.tree.server.kms.Kms;
import org.kurento.tree.server.kms.real.RealKms;

public class RealElasticKmsManager extends KmsManager implements KmsRegistrar {

	public List<Kms> kmss = new CopyOnWriteArrayList<>();

	public RealElasticKmsManager(List<String> kmsWsUris) throws IOException {
		for (String kmsWsUri : kmsWsUris) {
			addKms(kmsWsUri);
		}
	}

	private void addKms(String kmsWsUri) {
		this.kmss.add(new RealKms(KurentoClient.create(kmsWsUri)));
	}

	public List<Kms> getKmss() {
		return kmss;
	}

	@Override
	public void register(String kmsWsUri) {
		addKms(kmsWsUri);
	}
}
