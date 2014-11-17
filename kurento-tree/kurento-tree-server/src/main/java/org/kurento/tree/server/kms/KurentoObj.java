package org.kurento.tree.server.kms;

public class KurentoObj {

	private String label;
	protected boolean released = false;

	public KurentoObj(String label) {
		this.label = label;
	}

	public KurentoObj() {
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public void release() {
		released = true;
	}

	protected void checkReleased() {
		if (released) {
			throw new RuntimeException(
					"Trying to execute an operation in a released element");
		}
	}
}
