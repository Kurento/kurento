package org.kurento.tree.server.kmsmanager;

import java.util.ArrayList;
import java.util.List;

import org.kurento.tree.server.kms.Kms;
import org.kurento.tree.server.kms.loadmanager.LoadManager;

public class FakeFixedNKmsManager extends KmsManager {

	public List<Kms> kmss = new ArrayList<>();

	public FakeFixedNKmsManager(int numKmss) {
		this(numKmss, null);
	}

	public FakeFixedNKmsManager(int numKmss, LoadManager loadManager) {
		for (int i = 0; i < numKmss; i++) {
			Kms kms = new Kms();
			if (loadManager != null) {
				kms.setLoadManager(loadManager);
			}
			this.kmss.add(kms);
		}
	}

	public List<Kms> getKmss() {
		return kmss;
	}

}
