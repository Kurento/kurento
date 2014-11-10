package org.kurento.tree.server.treemanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.kurento.tree.client.TreeEndpoint;
import org.kurento.tree.server.kms.Kms;
import org.kurento.tree.server.kms.Pipeline;
import org.kurento.tree.server.kms.Plumber;
import org.kurento.tree.server.kms.WebRtc;
import org.kurento.tree.server.kmsmanager.KmsManager;
import org.kurento.tree.server.kmsmanager.KmsManager.KmsLoad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This TreeManager has the following characteristics:
 * <ul>
 * <li>Creates WebRtcEndpoint for sinks (viewers) only in non-source kmss</li>
 * <li>Fills less loaded node.</li>
 * <li>It doesn't consider new kmss after start.</li>
 * </ul>
 *
 * @author micael.gallego@gmail.com
 */
public class AotFixedTreeManager extends AbstractOneTreeManager {

	private static final Logger log = LoggerFactory
			.getLogger(AotFixedTreeManager.class);

	private KmsManager kmsManager;

	private boolean oneKms = true;

	private Pipeline sourcePipeline;
	private List<Plumber> sourcePlumbers = new ArrayList<>();
	private WebRtc source;

	private List<Pipeline> leafPipelines = new ArrayList<>();
	private List<Plumber> leafPlumbers = new ArrayList<>();
	private Map<String, WebRtc> sinks = new ConcurrentHashMap<>();

	private int numSinks = 0;

	public AotFixedTreeManager(KmsManager kmsManager) {

		this.kmsManager = kmsManager;

		if (kmsManager.getKmss().isEmpty()) {
			log.error("AotOneTreeManager cannot be used without initial kmss");

		} else if (kmsManager.getKmss().size() == 1) {

			oneKms = true;

			sourcePipeline = kmsManager.getKmss().get(0).createPipeline();

		} else {

			oneKms = false;

			int numPipeline = 0;
			for (Kms kms : kmsManager.getKmss()) {
				Pipeline pipeline = kms.createPipeline();

				if (sourcePipeline == null) {
					sourcePipeline = pipeline;
				} else {
					pipeline.setLabel(Integer.toString(numPipeline));
					leafPipelines.add(pipeline);
					Plumber[] plumbers = sourcePipeline.link(pipeline);
					this.sourcePlumbers.add(plumbers[0]);
					this.leafPlumbers.add(plumbers[1]);
					numPipeline++;
				}
			}
		}
	}

	@Override
	public KmsManager getKmsManager() {
		return kmsManager;
	}

	@Override
	public synchronized void releaseTree(String treeId) throws TreeException {

		checkTreeId(treeId);

		createdTree = false;
		source.release();
		source = null;

		for (WebRtc webRtc : sinks.values()) {
			webRtc.release();
		}
	}

	@Override
	public synchronized String setTreeSource(String treeId, String offerSdp)
			throws TreeException {

		checkTreeId(treeId);

		if (source != null) {
			removeTreeSource(treeId);
		}

		source = sourcePipeline.createWebRtc();

		if (!oneKms) {
			for (Plumber plumber : this.sourcePlumbers) {
				source.connect(plumber);
			}
		}

		return source.processSdpOffer(offerSdp);
	}

	@Override
	public synchronized void removeTreeSource(String treeId)
			throws TreeException {

		checkTreeId(treeId);
		source.release();
		source = null;
	}

	@Override
	public synchronized TreeEndpoint addTreeSink(String treeId, String sdpOffer)
			throws TreeException {

		checkTreeId(treeId);

		TreeEndpoint result = null;

		if (oneKms) {
			if (sourcePipeline.getKms().allowMoreElements()) {
				WebRtc webRtc = sourcePipeline.createWebRtc();
				source.connect(webRtc);
				String sdpAnswer = webRtc.processSdpOffer(sdpOffer);
				String id = "r_" + (sourcePipeline.getWebRtcs().size() - 1);
				webRtc.setLabel("Sink " + numSinks + " (WR " + id + ")");
				result = new TreeEndpoint(sdpAnswer, id);
			}
		} else {

			List<KmsLoad> kmss = kmsManager.getKmssSortedByLoad();

			Pipeline pipeline;
			if (kmss.get(0).getKms().getPipelines().get(0) != sourcePipeline) {
				pipeline = kmss.get(0).getKms().getPipelines().get(0);
			} else {
				pipeline = kmss.get(1).getKms().getPipelines().get(0);
			}

			if (pipeline.getKms().allowMoreElements()) {
				WebRtc webRtc = pipeline.createWebRtc();
				pipeline.getPlumbers().get(0).connect(webRtc);
				String sdpAnswer = webRtc.processSdpOffer(sdpOffer);
				String id = pipeline.getLabel() + "_"
						+ (pipeline.getWebRtcs().size() - 1);
				webRtc.setLabel("Sink " + numSinks + " (WR " + id + ")");
				result = new TreeEndpoint(sdpAnswer, id);
			} else {
				System.out.println("sss");
			}
		}

		if (result != null) {
			numSinks++;
			return result;
		} else {
			throw new TreeException("Max number of viewers reached");
		}
	}

	@Override
	public synchronized void removeTreeSink(String treeId, String sinkId)
			throws TreeException {

		checkTreeId(treeId);

		String[] sinkIdTokens = sinkId.split("_");

		if (sinkIdTokens[0].equals("r")) {

			int numWebRtc = Integer.parseInt(sinkIdTokens[1]);
			this.sourcePipeline.getWebRtcs().get(numWebRtc).disconnect();

		} else {
			int numPipeline = Integer.parseInt(sinkIdTokens[0]);
			int numWebRtc = Integer.parseInt(sinkIdTokens[1]);

			WebRtc webRtc = this.leafPipelines.get(numPipeline).getWebRtcs()
					.get(numWebRtc);
			webRtc.release();
		}
	}

}
