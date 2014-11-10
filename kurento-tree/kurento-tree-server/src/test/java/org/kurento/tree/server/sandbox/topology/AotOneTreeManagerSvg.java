package org.kurento.tree.server.sandbox.topology;

import org.kurento.tree.server.debug.KmsTopologyGrapher;
import org.kurento.tree.server.kmsmanager.FakeFixedKmsManager;
import org.kurento.tree.server.kmsmanager.KmsManager;
import org.kurento.tree.server.treemanager.AotFixedClientsNoRootTreeManager;

public class AotOneTreeManagerSvg {

	public static void main(String[] args) {

		KmsManager kmsManager = new FakeFixedKmsManager(4);
		AotFixedClientsNoRootTreeManager aot = new AotFixedClientsNoRootTreeManager(
				kmsManager);

		System.out.println(KmsTopologyGrapher
				.createSvgTopologyGrapher(kmsManager));

		String treeId = aot.createTree();
		aot.setTreeSource(treeId, "XXX");

		System.out.println(KmsTopologyGrapher
				.createSvgTopologyGrapher(kmsManager));

		aot.addTreeSink(treeId, "JJJ");
		aot.addTreeSink(treeId, "FFF");

		System.out.println(KmsTopologyGrapher
				.createSvgTopologyGrapher(kmsManager));
	}
}
