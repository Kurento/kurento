package org.kurento.tree.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.kurento.tree.protocol.TreeEndpoint;
import org.kurento.tree.server.kms.Kms;
import org.kurento.tree.server.kms.Pipeline;
import org.kurento.tree.server.kms.Plumber;
import org.kurento.tree.server.kms.WebRtc;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AotOneTreeManager implements TreeManager {

	private static final String TREE_ID = "TreeId";

	private static final Logger log = LoggerFactory
			.getLogger(AotOneTreeManager.class);

	private KmsManager kmsManager;
	private int maxViewersPerPipeline = 5;

	private boolean oneKms = true;

	private Pipeline rootPipeline;
	private List<Plumber> rootPlumbers = new ArrayList<>();
	private WebRtc sourceWebRtc;

	private List<Pipeline> leafPipelines = new ArrayList<>();
	private List<Plumber> leafPlumbers = new ArrayList<>();
	private Map<String, WebRtc> sinks = new ConcurrentHashMap<>();

	private boolean createdTree = false;

	public AotOneTreeManager(KmsManager kmsManager) {
		this(kmsManager, 5);
	}

	public AotOneTreeManager(KmsManager kmsManager, int maxViewersPerPipeline) {

		this.maxViewersPerPipeline = maxViewersPerPipeline;
		this.kmsManager = kmsManager;

		if (kmsManager.getKmss().isEmpty()) {
			log.error("AotOneTreeManager cannot be used without initial kmss");

		} else if (kmsManager.getKmss().size() == 1) {

			oneKms = true;

			rootPipeline = kmsManager.getKmss().get(0).createPipeline();

		} else {

			oneKms = false;

			for (Kms kms : kmsManager.getKmss()) {
				Pipeline pipeline = kms.createPipeline();
				if (rootPipeline == null) {
					rootPipeline = pipeline;
				} else {
					leafPipelines.add(pipeline);
					Plumber[] plumbers = rootPipeline.link(pipeline);
					this.rootPlumbers.add(plumbers[0]);
					this.leafPlumbers.add(plumbers[1]);
				}
			}
		}
	}

	@Override
	public synchronized String createTree() throws TreeException {
		if (createdTree) {
			throw new TreeException(
					"AotOneTreeManager "
							+ " can only create one tree and this tree was previously created");
		}
		return TREE_ID;
	}

	@Override
	public synchronized void releaseTree(String treeId) throws TreeException {

		checkTreeId(treeId);

		createdTree = false;
		sourceWebRtc.release();
		sourceWebRtc = null;

		for (WebRtc webRtc : sinks.values()) {
			webRtc.release();
		}
	}

	@Override
	public synchronized String setTreeSource(String treeId, String offerSdp)
			throws TreeException {

		checkTreeId(treeId);

		if (sourceWebRtc != null) {
			removeTreeSource(treeId);
		}

		sourceWebRtc = rootPipeline.createWebRtc();

		if (!oneKms) {
			for (Plumber plumber : this.rootPlumbers) {
				sourceWebRtc.connect(plumber);
			}
		}

		return sourceWebRtc.processSdpOffer(offerSdp);
	}

	@Override
	public synchronized void removeTreeSource(String treeId)
			throws TreeException {

		checkTreeId(treeId);

		sourceWebRtc.release();
		sourceWebRtc = null;
	}

	@Override
	public synchronized TreeEndpoint addTreeSink(String treeId, String sdpOffer)
			throws TreeException {

		checkTreeId(treeId);

		TreeEndpoint result = null;

		if (oneKms) {
			if (rootPipeline.getWebRtcs().size() < maxViewersPerPipeline) {
				WebRtc webRtc = rootPipeline.createWebRtc();
				sourceWebRtc.connect(webRtc);
				String sdpAnswer = webRtc.processSdpOffer(sdpOffer);
				result = new TreeEndpoint(sdpAnswer, "r_"
						+ (rootPipeline.getWebRtcs().size() - 1));
			} else {
				throw new TreeException("Max number of viewers reached");
			}
		} else {
			int numPipeline = 0;
			for (Pipeline pipeline : this.leafPipelines) {
				if (pipeline.getWebRtcs().size() < maxViewersPerPipeline) {
					WebRtc webRtc = pipeline.createWebRtc();
					pipeline.getPlumbers().get(0).connect(webRtc);
					String sdpAnswer = webRtc.processSdpOffer(sdpOffer);
					result = new TreeEndpoint(sdpAnswer, numPipeline + "_"
							+ (pipeline.getWebRtcs().size() - 1));
					break;
				}
				numPipeline++;
			}
		}

		if (result != null) {
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
			this.rootPipeline.getWebRtcs().get(numWebRtc).disconnect();

		} else {
			int numPipeline = Integer.parseInt(sinkIdTokens[0]);
			int numWebRtc = Integer.parseInt(sinkIdTokens[1]);

			this.leafPipelines.get(numPipeline).getWebRtcs().get(numWebRtc)
					.disconnect();
		}
	}

	private void checkTreeId(String treeId) throws TreeException {
		if (!TREE_ID.equals(treeId)) {
			throw new TreeException("Unknown tree '" + treeId + "'");
		}
	}

}
