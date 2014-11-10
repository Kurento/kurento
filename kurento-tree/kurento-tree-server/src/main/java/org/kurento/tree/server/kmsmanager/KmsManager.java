package org.kurento.tree.server.kmsmanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.kurento.tree.server.kms.Kms;

public abstract class KmsManager {

	public static class KmsLoad implements Comparable<KmsLoad> {

		private Kms kms;
		private double load;

		public KmsLoad(Kms kms, double load) {
			this.kms = kms;
			this.load = load;
		}

		public Kms getKms() {
			return kms;
		}

		public double getLoad() {
			return load;
		}

		@Override
		public int compareTo(KmsLoad o) {
			return Double.compare(this.load, o.load);
		}
	}

	public abstract List<Kms> getKmss();

	public Kms getLessLoadedKms() {
		ArrayList<KmsLoad> kmsLoads = new ArrayList<>();
		for (Kms kms : getKmss()) {
			kmsLoads.add(new KmsLoad(kms, kms.getLoad()));
		}
		return Collections.min(kmsLoads).kms;
	}

	public List<KmsLoad> getKmssSortedByLoad() {
		ArrayList<KmsLoad> kmsLoads = new ArrayList<>();
		for (Kms kms : getKmss()) {
			kmsLoads.add(new KmsLoad(kms, kms.getLoad()));
		}
		Collections.sort(kmsLoads);
		return kmsLoads;
	}

}
