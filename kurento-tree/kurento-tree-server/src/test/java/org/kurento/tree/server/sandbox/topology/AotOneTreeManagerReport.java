package org.kurento.tree.server.sandbox.topology;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.kurento.tree.client.TreeException;
import org.kurento.tree.server.debug.TreeManagerReportCreator;
import org.kurento.tree.server.kmsmanager.FakeFixedNKmsManager;
import org.kurento.tree.server.kmsmanager.KmsManager;
import org.kurento.tree.server.treemanager.LessLoadedOneTreeFixedTM;
import org.kurento.tree.server.treemanager.TreeManager;

public class AotOneTreeManagerReport {

	public static void main(String[] args) throws IOException {

		KmsManager kmsManager = new FakeFixedNKmsManager(4);

		TreeManager aot = new LessLoadedOneTreeFixedTM(kmsManager);

		TreeManagerReportCreator reportCreator = new TreeManagerReportCreator(
				kmsManager, "Report");

		reportCreator.setTreeManager(aot);

		aot = reportCreator;

		String treeId = aot.createTree();
		aot.setTreeSource(treeId, "XXX");

		List<String> sinks = new ArrayList<String>();
		try {
			while (true) {

				for (int i = 0; i < 5; i++) {
					sinks.add(aot.addTreeSink(treeId, "fakeSdp").getId());
				}

				for (int i = 0; i < 2; i++) {
					int sinkNumber = (int) (Math.random() * sinks.size());
					String sinkId = sinks.remove(sinkNumber);
					aot.removeTreeSink(treeId, sinkId);
				}
			}
		} catch (TreeException e) {
			System.out.println("Reached maximum tree capacity");
		}

		reportCreator.createReport("/home/mica/Data/Kurento/treereport.html");
	}
}
