package com.kurento.kmf.phone;

import java.util.concurrent.ConcurrentHashMap;

public class Registry {
	
	private ConcurrentHashMap<String, PhoneHandler> users = new ConcurrentHashMap<String, PhoneHandler>();

	public void register(String userName, PhoneHandler userPhoneHandler) {
		users.put(userName, userPhoneHandler);		
	}

	public PhoneHandler get(String userName) {
		return users.get(userName);
	}

}
