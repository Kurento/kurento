package org.kurento.tree.server.sandbox.topology;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.kurento.tree.server.debug.TreeManagerReportCreator;
import org.kurento.tree.server.kmsmanager.FakeFixedKmsManager;
import org.kurento.tree.server.kmsmanager.KmsManager;
import org.kurento.tree.server.treemanager.LessLoadedNElasticTreeManager;
import org.kurento.tree.server.treemanager.TreeManager;

public class LessLoadedNElasticTreeManagerReport {

	public static void main(String[] args) throws IOException {

		KmsManager kmsManager = new FakeFixedKmsManager(4);

		TreeManager aot = new LessLoadedNElasticTreeManager(kmsManager);

		TreeManagerReportCreator reportCreator = new TreeManagerReportCreator(
				kmsManager, "Report");

		reportCreator.setTreeManager(aot);

		aot = reportCreator;

		int numTrees = 3;

		for (int numTree = 0; numTree < numTrees; numTree++) {
			String treeId = getTreeId(numTree);
			aot.createTree(treeId);
			aot.setTreeSource(treeId, "XXX");
		}

		Random r = new Random(0);
		double addProb = 0.8;

		List<String> sinks = new ArrayList<String>();
		try {
			while (true) {

				if (r.nextDouble() < addProb) {
					int numTree = r.nextInt(numTrees);
					String treeId = getTreeId(numTree);
					String sinkId = aot.addTreeSink(treeId, "fakeSdp").getId();
					sinks.add(treeId + "|" + sinkId);
				} else {
					if (!sinks.isEmpty()) {
						int sinkNumber = (int) (r.nextDouble() * sinks.size());
						String sink = sinks.remove(sinkNumber);
						String[] parts = sink.split("\\|");
						aot.removeTreeSink(parts[0], parts[1]);
					}
				}
			}

		} catch (Exception e) {
			System.out.println("Exception: " + e.getClass().getName() + ":"
					+ e.getMessage());
		}

		reportCreator.createReport("/home/mica/Data/Kurento/treereport.html");
	}

	private static String getTreeId(int numTree) {
		return "Tree" + numTree;
	}
}
