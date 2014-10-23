package org.kurento.tree.sandbox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.kurento.tree.debug.KmsTopologyGrapher;
import org.kurento.tree.debug.TreeManagerReportCreator;
import org.kurento.tree.server.kms.Kms;
import org.kurento.tree.server.kms.Pipeline;
import org.kurento.tree.server.kms.Plumber;
import org.kurento.tree.server.kms.WebRtc;
import org.kurento.tree.server.kmsmanager.FakeFixedKmsManager;
import org.kurento.tree.server.kmsmanager.KmsManager;
import org.kurento.tree.server.treemanager.AotFixedClientsNoRootTreeManager;
import org.kurento.tree.server.treemanager.AotFixedTreeManager;
import org.kurento.tree.server.treemanager.TreeException;
import org.kurento.tree.server.treemanager.TreeManager;

public class KmssTopologyGrapher {

	public static void main(String[] args) throws IOException, TreeException {
		// showBasicTree();
		// showAotOneTreeManager();
		// showAotOneTreeManagerSvg();
		showAotOneTreeManagerReport();
	}

	private static void showBasicTree() throws IOException {

		int numLeafKmss = 3;
		int numViewersPerKms = 5;

		Kms rootKms = new Kms("Root Kms");
		Pipeline pipeline = rootKms.createPipeline();
		WebRtc master = pipeline.createWebRtc();
		List<Plumber> rootPlumbers = new ArrayList<>();
		for (int i = 0; i < numLeafKmss; i++) {
			Plumber plumber = pipeline.createPlumber();
			rootPlumbers.add(plumber);
			master.connect(plumber);
		}

		List<Kms> leafKmss = new ArrayList<>();
		List<Pipeline> leafPipelines = new ArrayList<>();
		for (int i = 0; i < numLeafKmss; i++) {
			Kms leafKms = new Kms();
			leafKmss.add(leafKms);

			Pipeline leafPipeline = leafKms.createPipeline();
			leafPipelines.add(leafPipeline);

			Plumber leafPlumber = leafPipeline.createPlumber();
			rootPlumbers.get(i).link(leafPlumber);

			for (int j = 0; j < numViewersPerKms; j++) {
				WebRtc webRtc = leafPipeline.createWebRtc();
				leafPlumber.connect(webRtc);
			}
		}

		List<Kms> kmss = new ArrayList<>();
		kmss.add(rootKms);
		kmss.addAll(leafKmss);

		KmsTopologyGrapher.showTopologyGraphic(kmss);
	}

	private static void showAotOneTreeManager() throws IOException,
			TreeException {

		KmsManager kmsManager = new FakeFixedKmsManager(4);
		AotFixedClientsNoRootTreeManager aot = new AotFixedClientsNoRootTreeManager(
				kmsManager);

		KmsTopologyGrapher.showTopologyGraphic(kmsManager);

		String treeId = aot.createTree();
		aot.setTreeSource(treeId, "XXX");

		KmsTopologyGrapher.showTopologyGraphic(kmsManager);

		aot.addTreeSink(treeId, "JJJ");
		aot.addTreeSink(treeId, "FFF");

		KmsTopologyGrapher.showTopologyGraphic(kmsManager);
	}

	private static void showAotOneTreeManagerSvg() throws IOException,
			TreeException {

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

	private static void showAotOneTreeManagerReport() throws IOException,
			TreeException {

		KmsManager kmsManager = new FakeFixedKmsManager(4);

		TreeManager aot = new AotFixedTreeManager(kmsManager);

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
