package org.kurento.test.monitor;

import java.lang.reflect.Method;

public class MonitorStats {

	protected boolean isGetter(Method method) {
		return (method.getName().startsWith("get")
				|| method.getName().startsWith("is"))
				&& !method.getName().equals("getClass");
	}

	protected String getGetterName(Method method) {
		String name = method.getName();
		if (name.startsWith("get")) {
			name = name.substring(3);
		} else if (name.startsWith("is")) {
			name = name.substring(2);
		}
		return name;
	}

}
