package org.kurento.tree.server.kms;

public class DummyLoadManager implements LoadManager {

	@Override
	public double calculateLoad(Kms kms) {
		return 0;
	}

	@Override
	public boolean allowMoreElements(Kms kms) {
		return true;
	}
}
