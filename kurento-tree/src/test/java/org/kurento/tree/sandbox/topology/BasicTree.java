package org.kurento.tree.sandbox.topology;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.kurento.tree.debug.KmsTopologyGrapher;
import org.kurento.tree.server.kms.Kms;
import org.kurento.tree.server.kms.Pipeline;
import org.kurento.tree.server.kms.Plumber;
import org.kurento.tree.server.kms.WebRtc;

public class BasicTree {

	public static void main(String[] args) throws IOException {

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

}
