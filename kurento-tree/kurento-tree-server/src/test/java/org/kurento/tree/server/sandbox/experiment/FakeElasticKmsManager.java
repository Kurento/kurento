package org.kurento.tree.server.sandbox.experiment;

import java.util.ArrayList;
import java.util.List;

import org.kurento.tree.server.kms.Kms;
import org.kurento.tree.server.kmsmanager.KmsManager;

public class FakeElasticKmsManager extends KmsManager {

	private double meanLoadToGrow;
	public List<Kms> kmss = new ArrayList<>();

	public FakeElasticKmsManager(double meanLoadToGrow) {
		this.meanLoadToGrow = meanLoadToGrow;
		kmss.add(new Kms());
	}

	public List<Kms> getKmss() {
		checkLoadAndUpdateKmss();
		return kmss;
	}

	private void checkLoadAndUpdateKmss() {

		double totalLoad = 0;
		for (Kms kms : kmss) {
			totalLoad += kms.getLoad();
		}
		if (totalLoad / this.kmss.size() > meanLoadToGrow) {
			this.kmss.add(new Kms());
		}
	}
}
