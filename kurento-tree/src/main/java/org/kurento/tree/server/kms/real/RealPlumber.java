package org.kurento.tree.server.kms.real;

import org.kurento.client.PlumberEndpoint;
import org.kurento.tree.server.kms.Element;
import org.kurento.tree.server.kms.Plumber;

public class RealPlumber extends Plumber implements RealElement {

	private PlumberEndpoint plumberEndpoint;

	public RealPlumber(RealPipeline pipeline) {
		super(pipeline);
		plumberEndpoint = new PlumberEndpoint.Builder(
				pipeline.getMediaPipeline()).build();
	}

	@Override
	public void link(Plumber plumber) {
		if (!(plumber instanceof RealPlumber)) {
			throw new RuntimeException(
					"A real plumber can not be linked to non real one");
		}

		super.link(plumber);
		RealPlumber realPlumber = (RealPlumber) plumber;
		String address = realPlumber.plumberEndpoint.getAddress();
		int port = realPlumber.plumberEndpoint.getPort();
		this.plumberEndpoint.link(address, port);
	}

	@Override
	public void release() {
		super.release();
		plumberEndpoint.release();
	}

	@Override
	public PlumberEndpoint getMediaElement() {
		return plumberEndpoint;
	}

	@Override
	public void connect(Element element) {
		if (!(element instanceof RealElement)) {
			throw new RuntimeException(
					"A real element can not be connected to non real one");
		}
		super.connect(element);
		plumberEndpoint.connect(((RealElement) element).getMediaElement());
	}
}
