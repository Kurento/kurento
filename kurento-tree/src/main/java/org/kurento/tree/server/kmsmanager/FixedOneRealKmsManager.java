package org.kurento.tree.server.kmsmanager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.kurento.client.KurentoClient;
import org.kurento.tree.server.kms.Kms;
import org.kurento.tree.server.kms.real.RealKms;

public class FixedOneRealKmsManager extends KmsManager {

	public List<Kms> kmss = new ArrayList<>();

	public FixedOneRealKmsManager(String kmsWsUri, int numKmss)
			throws IOException {
		for (int i = 0; i < numKmss; i++) {
			KurentoClient client = KurentoClient.create(kmsWsUri);
			this.kmss.add(new RealKms(client));
		}
	}

	public List<Kms> getKmss() {
		return kmss;
	}

}
