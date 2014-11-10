package org.kurento.tree.server.kmsmanager;

import java.util.ArrayList;
import java.util.List;

import org.kurento.tree.server.kms.Kms;

public class FakeFixedKmsManager extends KmsManager {

	public List<Kms> kmss = new ArrayList<>();

	public FakeFixedKmsManager(int numKmss) {
		for (int i = 0; i < numKmss; i++) {
			this.kmss.add(new Kms());
		}
	}

	public List<Kms> getKmss() {
		return kmss;
	}

}
