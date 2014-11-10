package org.kurento.tree.server.kms;

public interface LoadManager {

	double calculateLoad(Kms kms);

	boolean allowMoreElements(Kms kms);

}
