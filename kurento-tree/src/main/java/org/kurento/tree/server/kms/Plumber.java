package org.kurento.tree.server.kms;

public class Plumber extends Element {

	private Plumber linkedTo;

	Plumber(Pipeline pipeline) {
		super(pipeline);
	}

	public void link(Plumber plumber) {
		if (plumber.getPipeline().getKms() == this.getPipeline().getKms()) {
			throw new RuntimeException(
					"Two plumbers of the same Kms can not be linked");
		}

		if (this.linkedTo != null || plumber.linkedTo != null) {
			throw new RuntimeException("A plumber only can be connected once");
		}

		this.linkedTo = plumber;
		plumber.linkedTo = this;
	}

	public Plumber getLinkedTo() {
		return linkedTo;
	}
}
