package org.kurento.tree.server.kms.real;

import org.kurento.module.plumberendpoint.PlumberEndpoint;
import org.kurento.tree.server.kms.Element;
import org.kurento.tree.server.kms.Plumber;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RealPlumber extends Plumber implements RealElement {

	private static Logger log = LoggerFactory.getLogger(RealPlumber.class);
	
	private PlumberEndpoint plumberEndpoint;
	
	public RealPlumber(RealPipeline pipeline) {
		super(pipeline);
		plumberEndpoint =
				new PlumberEndpoint.Builder(pipeline.getMediaPipeline())
						.build();
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
	
	@Override
	public PlumberEndpoint getMediaElement() {
		return plumberEndpoint;
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
		log.debug("Connecting plumber to adress:" + address + " port:" + port);
		this.plumberEndpoint.link(address, port);
	}
	
	@Override
	public void release() {
		super.release();
		plumberEndpoint.release();
	}
}
