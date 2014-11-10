package org.kurento.tree.debug;

import java.awt.Desktop;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import org.kurento.tree.client.TreeEndpoint;
import org.kurento.tree.server.kmsmanager.KmsManager;
import org.kurento.tree.server.treemanager.TreeException;
import org.kurento.tree.server.treemanager.TreeManager;

public class TreeManagerReportCreator implements TreeManager {

	private KmsManager kmsManager;
	private TreeManager treeManager;
	private String name;

	private Path reportPath;
	private PrintWriter writer;

	public TreeManagerReportCreator(KmsManager manager, String name) {
		this.kmsManager = manager;
		this.name = name;
		initReport();
	}

	public void setTreeManager(TreeManager treeManager) {
		this.treeManager = treeManager;
		writer.println("<h1>TreeManager: " + treeManager.getClass().getName()
				+ "</h1>");
		includeTreeManagerSnapshot();
	}

	private void initReport() {

		try {
			reportPath = Files.createTempFile("TreeReport", ".html");
			writer = new PrintWriter(Files.newBufferedWriter(reportPath,
					StandardCharsets.UTF_8));
			writer.println("<html>");
			writer.println("<title>" + name + "</title>");
			writer.println("<body>");
			writer.println("<h1>" + name + "</h1>");

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// Public API

	public void addSection(String sectionName) {
		writer.println("<h2>" + sectionName + "</h2>");
	}

	public void createReport(String path) throws IOException {
		createReport();
		Files.move(this.reportPath, Paths.get(path),
				StandardCopyOption.REPLACE_EXISTING);
		this.reportPath = Paths.get(path);
	}

	public void createReport() {
		writer.println("</body>");
		writer.println("</html>");
		writer.close();
	}

	public void showReport() throws IOException {
		System.out.println("Opening report in " + reportPath);
		Desktop.getDesktop().browse(reportPath.toUri());
	}

	// Report generation operations

	private void includeOperation(String operation) {
		writer.println("<p>" + operation + "</p>");
		System.out.println(operation);
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

	public void addText(String text) {
		writer.println("<p>" + text + "</p>");
	}

	@Override
	public void createTree(String treeId) throws TreeException {
		treeManager.createTree(treeId);
		includeOperation("createTree(" + treeId + ")");
		includeTreeManagerSnapshot();
	}

}
