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
	private boolean ignoreFirstKmsInLoadMeasure;

	public FakeElasticKmsManager(double meanLoadToGrow, int minKmss, int maxKmss) {
		this(meanLoadToGrow, minKmss, maxKmss, null, true);
	}

	public FakeElasticKmsManager(double meanLoadToGrow, int minKmss,
			int maxKmss, LoadManager loadManager,
			boolean ignoreFirstKmsInLoadMeasure) {
		this.meanLoadToGrow = meanLoadToGrow;
		this.loadManager = loadManager;
		this.maxKmss = maxKmss;
		this.ignoreFirstKmsInLoadMeasure = ignoreFirstKmsInLoadMeasure;
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

			int numKms = 0;
			for (Kms kms : kmss) {
				if (!ignoreFirstKmsInLoadMeasure || numKms > 0) {
					totalLoad += kms.getLoad();
				}
				numKms++;
			}

			int kmssToMean = ignoreFirstKmsInLoadMeasure ? this.kmss.size() - 1
					: this.kmss.size();

			double meanLoad = totalLoad / kmssToMean;
			System.out.println("Mean load: " + meanLoad);
			if (meanLoad > meanLoadToGrow) {
				this.kmss.add(newKms());
			}
		}
	}
}
