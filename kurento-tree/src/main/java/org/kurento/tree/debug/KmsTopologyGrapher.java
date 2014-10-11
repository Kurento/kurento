package org.kurento.tree.debug;

import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.kurento.tree.server.KmsManager;
import org.kurento.tree.server.kms.Element;
import org.kurento.tree.server.kms.Kms;
import org.kurento.tree.server.kms.Pipeline;
import org.kurento.tree.server.kms.Plumber;
import org.kurento.tree.server.kms.WebRtc;

public class KmsTopologyGrapher {

	public static void showTopologyGraphic(KmsManager kmsManager)
			throws IOException {
		showTopologyGraphic(kmsManager.getKmss());
	}

	public static void showTopologyGraphic(List<Kms> kmss) throws IOException {

		GraphViz gv = generateTopologyGraphViz(kmss);

		// System.out.println(gv.getDotSource());

		Path file = Files.createTempFile("kmsTopology", ".pdf");

		gv.writeGraphToFile(gv.getGraph(gv.getDotSource(), "pdf"),
				file.toFile());

		Desktop.getDesktop().open(file.toFile());
	}

	public static String createSvgTopologyGrapher(KmsManager manager) {
		return createSvgTopologyGrapher(manager, true);
	}

	public static String createSvgTopologyGrapher(KmsManager manager,
			boolean xmlHeader) {

		GraphViz gv = generateTopologyGraphViz(manager.getKmss());

		String svg = new String(gv.getGraph(gv.getDotSource(), "svg"));
		if (!xmlHeader) {
			int titleIndex = svg.indexOf("<!-- Title:");
			svg = svg.substring(titleIndex);
		}

		return svg;
	}

	private static GraphViz generateTopologyGraphViz(List<Kms> kmss) {

		GraphViz gv = new GraphViz();
		gv.addln(gv.startGraph());

		gv.addln("  rankdir=LR;");
		gv.addln("  node [shape=rectangle, style=filled];");

		// Elements
		Map<Object, String> labels = generateLabels(kmss);
		List<Plumber> plumbers = new ArrayList<>();

		int numKms = 0;
		for (Kms kms : kmss) {

			gv.addln("   subgraph cluster_kms_" + numKms + " {");
			gv.addln("      label = \"" + labels.get(kms) + "\";");

			int numPipeline = 0;
			for (Pipeline pipeline : kms.getPipelines()) {
				gv.addln("      subgraph cluster_pipeline_" + numKms + "_"
						+ numPipeline + " {");
				gv.addln("         label = \"" + labels.get(pipeline) + "\";");
				gv.addln("         style=filled;");
				gv.addln("         color=lightblue;");

				for (WebRtc webRtc : pipeline.getWebRtcs()) {
					for (Element sink : webRtc.getSinks()) {
						gv.addln("         \"" + labels.get(webRtc)
								+ "\" -> \"" + labels.get(sink) + "\"");
					}
				}

				plumbers.addAll(pipeline.getPlumbers());
				for (Plumber plumber : pipeline.getPlumbers()) {
					gv.addln("         \"" + labels.get(plumber) + "\"");
					for (Element sink : plumber.getSinks()) {
						gv.addln("         \"" + labels.get(plumber)
								+ "\" -> \"" + labels.get(sink) + "\"");
					}
				}

				gv.addln("      }");
				numPipeline++;
			}
			gv.addln("   }");
			numKms++;
		}

		Set<Plumber> connectedPlumbers = new HashSet<>();
		for (Plumber plumber : plumbers) {
			if (!connectedPlumbers.contains(plumber)) {
				gv.addln("   \"" + labels.get(plumber) + "\" -> \""
						+ labels.get(plumber.getLinkedTo()) + "\";");
				connectedPlumbers.add(plumber);
				connectedPlumbers.add(plumber.getLinkedTo());
			}
		}

		gv.addln(gv.endGraph());

		return gv;
	}

	private static Map<Object, String> generateLabels(List<Kms> kmss) {

		Map<Object, String> labels = new HashMap<>();

		int numKms = 0;
		for (Kms kms : kmss) {

			labels.put(kms, getKmsLabel(kms, numKms));

			int numPipeline = 0;
			for (Pipeline pipeline : kms.getPipelines()) {

				labels.put(pipeline,
						getPipelineLabel(pipeline, numKms, numPipeline));

				int numWebRtc = 0;
				for (WebRtc webRtc : pipeline.getWebRtcs()) {
					labels.put(
							webRtc,
							getWebRtcLabel(webRtc, numKms, numPipeline,
									numWebRtc));
					numWebRtc++;
				}

				int numPlumber = 0;
				for (Plumber plumber : pipeline.getPlumbers()) {
					labels.put(
							plumber,
							getPlumberLabel(plumber, numKms, numPipeline,
									numPlumber));
					numPlumber++;
				}
				numPipeline++;
			}
			numKms++;
		}

		return labels;
	}

	private static String getPlumberLabel(Plumber plumber, int numKms,
			int numPipeline, int numWebRtc) {
		String label = plumber.getLabel();
		if (label == null) {
			label = "plumber " + numKms + "." + numPipeline + "." + numWebRtc;
		}
		return label;
	}

	private static String getWebRtcLabel(WebRtc webRtc, int numKms,
			int numPipeline, int numWebRtc) {
		String label = webRtc.getLabel();
		if (label == null) {
			label = "webRtc " + numKms + "." + numPipeline + "." + numWebRtc;
		}
		return label;
	}

	private static String getPipelineLabel(Pipeline pipeline, int numKms,
			int numPipeline) {
		String label = pipeline.getLabel();
		if (label == null) {
			label = "pipeline " + numKms + "." + numPipeline;
		}
		return label;
	}

	private static String getKmsLabel(Kms kms, int numKms) {
		String label = kms.getLabel();
		if (label == null) {
			label = "kms " + numKms;
		}
		return label;
	}

}
