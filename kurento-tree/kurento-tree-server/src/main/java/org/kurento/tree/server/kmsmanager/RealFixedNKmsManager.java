package org.kurento.tree.server.kmsmanager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.kurento.client.KurentoClient;
import org.kurento.tree.server.kms.Kms;
import org.kurento.tree.server.kms.real.RealKms;

public class RealFixedNKmsManager extends KmsManager {

	public List<Kms> kmss = new ArrayList<>();

	public RealFixedNKmsManager(List<String> kmsWsUris) throws IOException {
		for (String kmsWsUri : kmsWsUris) {
			this.kmss.add(new RealKms(KurentoClient.create(kmsWsUri)));
		}
	}

	public List<Kms> getKmss() {
		return kmss;
	}

}
