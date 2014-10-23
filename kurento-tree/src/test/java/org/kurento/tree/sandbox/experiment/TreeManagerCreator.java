package org.kurento.tree.sandbox.experiment;

import org.kurento.tree.server.kmsmanager.KmsManager;
import org.kurento.tree.server.treemanager.TreeManager;

public interface TreeManagerCreator {

	public TreeManager createTreeManager(KmsManager kmsManager);
}
