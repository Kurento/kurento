package org.kurento.tree.sandbox;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.kurento.tree.debug.KmsTopologyGrapher;
import org.kurento.tree.debug.TreeManagerReportCreator;
import org.kurento.tree.server.AotOneTreeManager;
import org.kurento.tree.server.FixedFakeKmsManager;
import org.kurento.tree.server.KmsManager;
import org.kurento.tree.server.TreeException;
import org.kurento.tree.server.TreeManager;
import org.kurento.tree.server.kms.Kms;
import org.kurento.tree.server.kms.Pipeline;
import org.kurento.tree.server.kms.Plumber;
import org.kurento.tree.server.kms.WebRtc;

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

		KmsManager kmsManager = new FixedFakeKmsManager(4);
		AotOneTreeManager aot = new AotOneTreeManager(kmsManager);

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

		KmsManager kmsManager = new FixedFakeKmsManager(4);
		AotOneTreeManager aot = new AotOneTreeManager(kmsManager);

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

		KmsManager kmsManager = new FixedFakeKmsManager(4);
		TreeManager aot = new AotOneTreeManager(kmsManager);

		TreeManagerReportCreator reportCreator = new TreeManagerReportCreator(
				aot, kmsManager, "Report");

		aot = reportCreator;

		String treeId = aot.createTree();
		aot.setTreeSource(treeId, "XXX");
		String sink1 = aot.addTreeSink(treeId, "JJJ").getId();
		String sink2 = aot.addTreeSink(treeId, "FFF").getId();
		String sink3 = aot.addTreeSink(treeId, "ZZZ").getId();
		aot.removeTreeSink(treeId, sink2);
		aot.removeTreeSink(treeId, sink3);

		reportCreator.finishReport();

		reportCreator.showReport();
	}

}
