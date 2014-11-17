package org.kurento.tree.server.kms.loadmanager;

import org.kurento.tree.server.kms.Kms;

public interface LoadManager {

	double calculateLoad(Kms kms);

	boolean allowMoreElements(Kms kms);

}
