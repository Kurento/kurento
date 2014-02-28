package com.kurento.tool.rom.client;

import com.kurento.kmf.jsonrpcconnector.Props;
import com.kurento.tool.rom.server.RomException;

public class RemoteObjectFactory {

	private RomClientObjectManager manager = new RomClientObjectManager();
	
	private RomClient client;
	
	public RemoteObjectFactory(RomClient client) {
		this.client = client;
		this.client.addRomEventHandler(manager);
	}
	
	public RemoteObject create(String remoteClassName,
			Props constructorParams) throws RomException {
		
		String objectRef = client.create(remoteClassName, constructorParams);
		
		return new RemoteObject(objectRef, remoteClassName, client, manager);		
	}

	public RemoteObject create(String remoteClassName)
			throws RomException {

		return create(remoteClassName, null);
	}

	public void destroy() {
		this.client.destroy();		
	}
	
}
