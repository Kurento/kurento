package org.kurento.tree.server.kmsmanager;

import java.util.ArrayList;
import java.util.List;

import org.kurento.tree.server.kms.Kms;
import org.kurento.tree.server.kms.loadmanager.LoadManager;

public class FakeElasticKmsManager extends KmsManager {

	private double meanLoadToGrow;
	public List<Kms> kmss = new ArrayList<>();
	private LoadManager loadManager;
	private int maxKmss;
	private int minKmss;

	public FakeElasticKmsManager(double meanLoadToGrow, int minKmss, int maxKmss) {
		this(meanLoadToGrow, minKmss, maxKmss, null);
	}

	public FakeElasticKmsManager(double meanLoadToGrow, int minKmss,
			int maxKmss, LoadManager loadManager) {
		this.meanLoadToGrow = meanLoadToGrow;
		this.loadManager = loadManager;
		this.minKmss = minKmss;
		this.maxKmss = maxKmss;
		for (int i = 0; i < minKmss; i++) {
			kmss.add(newKms());
		}
	}

	private Kms newKms() {
		Kms kms = new Kms();
		if (loadManager != null) {
			kms.setLoadManager(loadManager);
		}
		return kms;
	}

	public List<Kms> getKmss() {
		checkLoadAndUpdateKmss();
		return kmss;
	}

	private void checkLoadAndUpdateKmss() {

		if (kmss.size() < maxKmss) {
			double totalLoad = 0;
			for (Kms kms : kmss) {
				totalLoad += kms.getLoad();
			}
			if (totalLoad / this.kmss.size() > meanLoadToGrow) {
				this.kmss.add(newKms());
			}
		}
	}
}
