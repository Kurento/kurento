package org.kurento.tree.debug;

import java.awt.Desktop;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.kurento.tree.protocol.TreeEndpoint;
import org.kurento.tree.server.KmsManager;
import org.kurento.tree.server.TreeException;
import org.kurento.tree.server.TreeManager;

public class TreeManagerReportCreator implements TreeManager {

	private KmsManager kmsManager;
	private TreeManager treeManager;
	private String name;

	private Path reportPath;
	private PrintWriter writer;

	public TreeManagerReportCreator(TreeManager treeManager,
			KmsManager manager, String name) throws IOException {
		this.kmsManager = manager;
		this.treeManager = treeManager;
		this.name = name;
		initReport();
	}

	private void initReport() throws IOException {
		reportPath = Files.createTempFile("TreeReport", ".htm");

		writer = new PrintWriter(Files.newBufferedWriter(reportPath,
				StandardCharsets.UTF_8));

		writer.println("<html>");
		writer.println("<title>" + name + "</title>");
		writer.println("<body>");
		writer.println("<h1>" + name + "</h1>");

		includeTreeManagerSnapshot();
	}

	// Public API

	public void finishReport() {
		writer.println("</body>");
		writer.println("</html>");
		writer.close();
	}

	public void showReport() throws IOException {
		Desktop.getDesktop().browse(reportPath.toUri());
	}

	// Report generation operations

	private void includeOperation(String operation) {
		writer.println("<p>" + operation + "</p>");
	}

	private void includeTreeManagerSnapshot() {
		writer.println(KmsTopologyGrapher.createSvgTopologyGrapher(kmsManager,
				false));
	}

	// TreeManager API

	@Override
	public String createTree() throws TreeException {
		String treeId = treeManager.createTree();
		includeOperation("createTree() -> " + treeId);
		includeTreeManagerSnapshot();
		return treeId;
	}

	@Override
	public void releaseTree(String treeId) throws TreeException {
		treeManager.releaseTree(treeId);
		includeOperation("releaseTree(" + treeId + ")");
		includeTreeManagerSnapshot();
	}

	@Override
	public String setTreeSource(String treeId, String sdpOffer)
			throws TreeException {
		String answerSdp = treeManager.setTreeSource(treeId, sdpOffer);
		includeOperation("setTreeSource(" + treeId + "," + sdpOffer + ") -> "
				+ answerSdp);
		includeTreeManagerSnapshot();
		return answerSdp;
	}

	@Override
	public void removeTreeSource(String treeId) throws TreeException {
		treeManager.removeTreeSource(treeId);
		includeOperation("removeTreeSource(" + treeId + ")");
		includeTreeManagerSnapshot();
	}

	@Override
	public TreeEndpoint addTreeSink(String treeId, String sdpOffer)
			throws TreeException {
		TreeEndpoint endpoint = treeManager.addTreeSink(treeId, sdpOffer);
		includeOperation("addTreeSink(" + treeId + "," + sdpOffer + ") -> "
				+ "sdp = " + endpoint.getSdp() + " , sinkId = "
				+ endpoint.getId());
		includeTreeManagerSnapshot();
		return endpoint;
	}

	@Override
	public void removeTreeSink(String treeId, String sinkId)
			throws TreeException {
		treeManager.removeTreeSink(treeId, sinkId);
		includeOperation("removeTreeSink(" + treeId + "," + sinkId + ")");
		includeTreeManagerSnapshot();
	}

	@Override
	public KmsManager getKmsManager() {
		return kmsManager;
	}
}
