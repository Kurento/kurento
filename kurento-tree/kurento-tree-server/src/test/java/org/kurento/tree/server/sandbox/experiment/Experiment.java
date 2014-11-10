package org.kurento.tree.server.sandbox.experiment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.kurento.tree.client.TreeException;
import org.kurento.tree.server.debug.TreeManagerReportCreator;
import org.kurento.tree.server.kmsmanager.FakeFixedKmsManager;
import org.kurento.tree.server.kmsmanager.KmsManager;
import org.kurento.tree.server.sandbox.UsageSimulation;
import org.kurento.tree.server.treemanager.TreeManager;

public abstract class Experiment {

	private List<UsageSimulation> usageSimulations = new ArrayList<>();
	private List<TreeManagerCreator> treeManagerCreators = new ArrayList<>();
	private KmsManager kmsManager = new FakeFixedKmsManager(4);

	public abstract void configureExperiment();

	protected void addUsageSimulation(UsageSimulation usageSimulation) {
		usageSimulations.add(usageSimulation);
	}

	protected void addTreeManagerCreator(TreeManagerCreator treeManagerCreator) {
		treeManagerCreators.add(treeManagerCreator);
	}

	protected void setKmsManager(KmsManager kmsManager) {
		this.kmsManager = kmsManager;
	}

	public void run() {

		configureExperiment();

		TreeManagerReportCreator reportCreator = new TreeManagerReportCreator(
				kmsManager, "Report");

		reportCreator.addText("KmsManager: " + kmsManager.getClass().getName());

		for (TreeManagerCreator treeManagerCreator : treeManagerCreators) {

			for (UsageSimulation simulation : usageSimulations) {

				reportCreator.addSection("Simulation "
						+ simulation.getClass().getName());

				TreeManager treeManager = treeManagerCreator
						.createTreeManager(kmsManager);

				reportCreator.setTreeManager(treeManager);

				try {
					simulation.useTreeManager(reportCreator);
				} catch (TreeException e) {
					System.out
							.println("Reached maximum tree capacity in TreeManager: "
									+ treeManager.getClass().getName()
									+ " and UsageSimulation: "
									+ simulation.getClass().getName());
				}
			}
		}

		try {
			reportCreator
					.createReport("/home/mica/Data/Kurento/treereport.html");
			System.out.println("Report created");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
