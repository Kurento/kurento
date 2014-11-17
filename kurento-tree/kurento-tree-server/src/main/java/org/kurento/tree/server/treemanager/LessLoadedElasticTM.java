package org.kurento.tree.server.treemanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.kurento.commons.exception.KurentoException;
import org.kurento.tree.client.TreeEndpoint;
import org.kurento.tree.client.TreeException;
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
 * <li>It allows N trees</li>
 * <li>Creates WebRtcEndpoint for sinks (viewers) only in non-source KMSs</li>
 * <li>Fills less loaded node.</li>
 * <li>It considers new KMSs after start.</li>
 * </ul>
 *
 * @author micael.gallego@gmail.com
 */
public class LessLoadedElasticTM extends AbstractNTreeTM {

	private static final Logger log = LoggerFactory
			.getLogger(LessLoadedElasticTM.class);

	public class LessLoadedTreeInfo extends TreeInfo {

		private KmsManager kmsManager;

		private String treeId;

		private Kms sourceKms;

		private Pipeline sourcePipeline;
		private List<Plumber> sourcePlumbers = new ArrayList<>();
		private WebRtc source;

		private List<Pipeline> leafPipelines = new ArrayList<>();
		private List<Plumber> leafPlumbers = new ArrayList<>();
		private Map<String, WebRtc> sinks = new ConcurrentHashMap<>();

		private Map<Kms, Pipeline> ownPipelineByKms = new ConcurrentHashMap<>();
		private Map<String, WebRtc> webRtcsById = new ConcurrentHashMap<>();

		private int numSinks = 0;

		public LessLoadedTreeInfo(String treeId, KmsManager kmsManager) {

			this.treeId = treeId;
			this.kmsManager = kmsManager;

			if (kmsManager.getKmss().isEmpty()) {
				throw new KurentoException(
						"LessLoadedNElasticTM cannot be used without initial kmss");
			}

			sourceKms = kmsManager.getKmss().get(0);
		}

		public void release() {
			source.release();
			for (WebRtc webRtc : sinks.values()) {
				webRtc.release();
			}
		}

		public String setTreeSource(String offerSdp) {

			if (source != null) {
				removeTreeSource();
			}

			if (sourcePipeline == null) {
				sourcePipeline = sourceKms.createPipeline();
				ownPipelineByKms.put(sourceKms, sourcePipeline);
			}

			source = sourcePipeline.createWebRtc();
			return source.processSdpOffer(offerSdp);
		}

		public void removeTreeSource() {
			source.release();
			source = null;
		}

		public TreeEndpoint addTreeSink(String sdpOffer) {

			List<KmsLoad> kmss = kmsManager.getKmssSortedByLoad();

			Kms selectedKms = kmss.get(0).getKms();
			if (selectedKms == sourceKms) {
				selectedKms = kmss.get(1).getKms();
			}

			Pipeline pipeline = getOrCreatePipeline(selectedKms);

			if (pipeline.getKms().allowMoreElements()) {

				WebRtc webRtc = pipeline.createWebRtc();
				pipeline.getPlumbers().get(0).connect(webRtc);
				String sdpAnswer = webRtc.processSdpOffer(sdpOffer);
				String id = UUID.randomUUID().toString();
				webRtcsById.put(id, webRtc);
				webRtc.setLabel("Sink " + id + ")");
				return new TreeEndpoint(sdpAnswer, id);

			} else {
				throw new TreeException("Max number of viewers reached");
			}
		}

		private Pipeline getOrCreatePipeline(Kms kms) {

			Pipeline pipeline = ownPipelineByKms.get(kms);

			if (pipeline == null) {

				pipeline = kms.createPipeline();

				ownPipelineByKms.put(kms, pipeline);

				pipeline.setLabel(UUID.randomUUID().toString());
				leafPipelines.add(pipeline);
				Plumber[] plumbers = sourcePipeline.link(pipeline);
				source.connect(plumbers[0]);
				this.sourcePlumbers.add(plumbers[0]);
				this.leafPlumbers.add(plumbers[1]);
			}

			return pipeline;
		}

		public void removeTreeSink(String sinkId) {
			WebRtc webRtc = webRtcsById.get(sinkId);
			Plumber plumber = (Plumber) webRtc.getSource();
			webRtc.release();
			if (plumber.getSinks().isEmpty()) {
				Plumber remotePlumber = plumber.getLinkedTo();
				Pipeline pipeline = plumber.getPipeline();
				ownPipelineByKms.remove(pipeline.getKms());
				pipeline.release();
				remotePlumber.release();
			}
		}
	}

	private KmsManager kmsManager;

	public LessLoadedElasticTM(KmsManager kmsManager) {
		this.kmsManager = kmsManager;
	}

	@Override
	public KmsManager getKmsManager() {
		return kmsManager;
	}

	@Override
	protected TreeInfo createTreeInfo(String treeId) {
		return new LessLoadedTreeInfo(treeId, kmsManager);
	}

}
