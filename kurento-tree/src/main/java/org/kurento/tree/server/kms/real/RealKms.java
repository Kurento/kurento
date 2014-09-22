package org.kurento.tree.server.kms.real;

import org.kurento.client.KurentoClient;
import org.kurento.tree.server.kms.Kms;
import org.kurento.tree.server.kms.Pipeline;

public class RealKms extends Kms {

	private KurentoClient client;

	public RealKms(KurentoClient client) {
		this.client = client;
	}

	protected Pipeline newPipeline() {
		return new RealPipeline(this);
	}

	public KurentoClient getKurentoClient() {
		return client;
	}

}
