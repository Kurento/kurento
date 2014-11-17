package org.kurento.tree.server.treemanager;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.kurento.commons.exception.KurentoException;
import org.kurento.tree.client.TreeEndpoint;
import org.kurento.tree.client.TreeException;
import org.kurento.tree.server.kms.Pipeline;
import org.kurento.tree.server.kms.WebRtc;
import org.kurento.tree.server.kmsmanager.KmsManager;

/**
 * This TreeManager has the following characteristics:
 * <ul>
 * <li>It uses only one KMS</li>
 * <li>It doesn't use any plumbers</li>
 * <li>It doesn't consider new KMSs after start.</li>
 * </ul>
 *
 * @author micael.gallego@gmail.com
 */
public class OneKmsTM extends AbstractNTreeTM {

	public class LessLoadedTreeInfo extends TreeInfo {

		private Pipeline pipeline;

		private WebRtc source;
		private Map<String, WebRtc> sinksById = new ConcurrentHashMap<>();

		public LessLoadedTreeInfo(String treeId, KmsManager kmsManager) {

			if (kmsManager.getKmss().isEmpty()) {
				throw new KurentoException(OneKmsTM.class.getName()
						+ " cannot be used without initial kmss");

			} else if (kmsManager.getKmss().size() == 1) {
				pipeline = kmsManager.getKmss().get(0).createPipeline();
			} else {
				throw new KurentoException(
						OneKmsTM.class.getName()
								+ " is designed to use only one KMS. Please use another TreeManager if you want to use several KMSs");
			}
		}

		public void release() {
			source.release();
			for (WebRtc webRtc : sinksById.values()) {
				webRtc.release();
			}
		}

		public String setTreeSource(String offerSdp) {
			if (source != null) {
				removeTreeSource();
			}
			source = pipeline.createWebRtc();
			return source.processSdpOffer(offerSdp);
		}

		public void removeTreeSource() {
			source.release();
			source = null;
		}

		public TreeEndpoint addTreeSink(String sdpOffer) {

			if (pipeline.getKms().allowMoreElements()) {

				WebRtc webRtc = pipeline.createWebRtc();
				source.connect(webRtc);
				String sdpAnswer = webRtc.processSdpOffer(sdpOffer);
				String id = UUID.randomUUID().toString();
				sinksById.put(id, webRtc);
				webRtc.setLabel("Sink " + id + ")");
				return new TreeEndpoint(sdpAnswer, id);

			} else {
				throw new TreeException("Max number of viewers reached");
			}
		}

		public void removeTreeSink(String sinkId) {
			WebRtc webRtc = sinksById.get(sinkId);
			webRtc.release();
		}
	}

	private KmsManager kmsManager;

	public OneKmsTM(KmsManager kmsManager) {
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
