package org.kurento.tree.server.treemanager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
public class LessLoadedNElasticTreeManager extends AbstractNTreeManager {

	private static final Logger log = LoggerFactory
			.getLogger(LessLoadedNElasticTreeManager.class);

	public class LessLoadedTreeInfo extends TreeInfo {

		private KmsManager kmsManager;

		private boolean oneKms = true;

		private String treeId;

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
				log.error("AotOneTreeManager cannot be used without initial kmss");

			} else if (kmsManager.getKmss().size() == 1) {

				oneKms = true;

				sourcePipeline = kmsManager.getKmss().get(0).createPipeline();

			} else {

				oneKms = false;

				int numPipeline = 0;
				for (Kms kms : kmsManager.getKmss()) {
					Pipeline pipeline = kms.createPipeline();

					ownPipelineByKms.put(kms, pipeline);

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

			source = sourcePipeline.createWebRtc();

			if (!oneKms) {
				for (Plumber plumber : this.sourcePlumbers) {
					source.connect(plumber);
				}
			}

			return source.processSdpOffer(offerSdp);
		}

		public void removeTreeSource() {
			source.release();
			source = null;
		}

		public TreeEndpoint addTreeSink(String sdpOffer) {

			TreeEndpoint result = null;

			if (oneKms) {

				if (sourcePipeline.getKms().allowMoreElements()) {
					WebRtc webRtc = sourcePipeline.createWebRtc();
					source.connect(webRtc);
					String sdpAnswer = webRtc.processSdpOffer(sdpOffer);
					String id = UUID.randomUUID().toString();
					webRtcsById.put(id, webRtc);
					webRtc.setLabel("Sink " + id + ")");
					result = new TreeEndpoint(sdpAnswer, id);
				}

			} else {

				List<KmsLoad> kmss = kmsManager.getKmssSortedByLoad();

				Pipeline pipeline = ownPipelineByKms.get(kmss.get(0).getKms());

				if (pipeline == sourcePipeline) {
					pipeline = ownPipelineByKms.get(kmss.get(1).getKms());
				}

				if (pipeline.getKms().allowMoreElements()) {

					WebRtc webRtc = pipeline.createWebRtc();
					pipeline.getPlumbers().get(0).connect(webRtc);
					String sdpAnswer = webRtc.processSdpOffer(sdpOffer);
					String id = UUID.randomUUID().toString();
					webRtcsById.put(id, webRtc);
					webRtc.setLabel("Sink " + id + ")");
					result = new TreeEndpoint(sdpAnswer, id);
				}
			}

			System.out.println("Before:" + this.leafPipelines);

			if (result != null) {
				numSinks++;
				return result;
			} else {
				throw new TreeException("Max number of viewers reached");
			}
		}

		public void removeTreeSink(String sinkId) {
			WebRtc webRtc = webRtcsById.get(sinkId);
			webRtc.release();
		}
	}

	private KmsManager kmsManager;

	public LessLoadedNElasticTreeManager(KmsManager kmsManager) {
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
