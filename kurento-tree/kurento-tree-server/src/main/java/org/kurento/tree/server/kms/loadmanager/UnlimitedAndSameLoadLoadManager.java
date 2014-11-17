package org.kurento.tree.server.kms.loadmanager;

import org.kurento.tree.server.kms.Kms;

public class UnlimitedAndSameLoadLoadManager implements LoadManager {

	@Override
	public double calculateLoad(Kms kms) {
		return 0;
	}

	@Override
	public boolean allowMoreElements(Kms kms) {
		return true;
	}
}
