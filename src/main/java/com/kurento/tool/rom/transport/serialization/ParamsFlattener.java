package com.kurento.tool.rom.transport.serialization;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.kurento.kmf.jsonrpcconnector.Prop;
import com.kurento.kmf.jsonrpcconnector.Props;
import com.kurento.tool.rom.client.RemoteObject;
import com.kurento.tool.rom.client.RemoteObjectInvocationHandler;

public class ParamsFlattener {

	private static final Logger LOG = LoggerFactory
			.getLogger(ParamsFlattener.class);

	private static final ParamsFlattener INSTANCE = new ParamsFlattener();

	public static ParamsFlattener getInstance() {
		return INSTANCE;
	}

	/**
	 * Flatten the parameter list to be sent to remote server using flattenParam
	 * method
	 * 
	 * @param params
	 * @return
	 */
	public Props flattenParams(Props params) {

		Props properties = new Props();
		for (Prop prop : params) {
			properties.add(prop.getName(), flattenParam(prop.getValue()));
		}
		return properties;
	}

	/**
	 * Flatten the parameter list to be sent to remote server using flattenParam
	 * method
	 * 
	 * @param params
	 * @return
	 */
	private List<?> flattenParamsList(List<? extends Object> params) {
		List<Object> plainParams = new ArrayList<Object>(params.size());
		for (Object param : params) {
			plainParams.add(flattenParam(param));
		}
		return plainParams;
	}

	/**
	 * Flatten param to be sent to remote server. The rules to flatten objects
	 * are:
	 * <ul>
	 * <li>If param is primitive (String, Boolean, Float or Integer) is not
	 * modified</li>
	 * <li>If param is enum value, is transformed to its String representation</li>
	 * <li>If param is an RemoteObject, is sent is reference String</li>
	 * <li>If param is a complex object, a Props object is created for it. The
	 * Props object has an entry for each property with its name and value. The
	 * value of the property is also flatten.</li>
	 * </ul>
	 * 
	 * @param param
	 * @return
	 */
	private Object flattenParam(Object param) {

		if (param == null) {
			return null;
		}

		Object processedParam;
		if (param instanceof RemoteObject) {
			processedParam = ((RemoteObject) param).getObjectRef();
		} else if (param instanceof Proxy) {

			InvocationHandler handler = Proxy.getInvocationHandler(param);
			if (handler instanceof RemoteObjectInvocationHandler) {
				RemoteObjectInvocationHandler roHandler = (RemoteObjectInvocationHandler) handler;
				processedParam = roHandler.getRemoteObject().getObjectRef();
			} else {
				throw new RuntimeException(
						"Only proxies from remote objects are allowed, but found one with InvocationHandler "
								+ handler);
			}

		} else if (param instanceof Enum<?>) {
			processedParam = param.toString();
		} else if (isPrimitive(param)) {
			processedParam = param;
		} else if (param instanceof List<?>) {
			processedParam = flattenParamsList((List<?>) param);
		} else {
			processedParam = extractObjectAsProps(param);
		}
		return processedParam;
	}

	/**
	 * Extract the bean properties of this param as Props object.
	 * 
	 * @param param
	 * @return
	 */
	private Object extractObjectAsProps(Object param) {

		Map<String, Object> propsMap = new HashMap<String, Object>();
		for (Method method : param.getClass().getMethods()) {

			String propName = null;

			String methodName = method.getName();
			if (methodName.startsWith("is")) {
				propName = methodName.substring(2, methodName.length());
			} else if (methodName.startsWith("get")
					&& !methodName.equals("getClass")) {
				propName = methodName.substring(3, methodName.length());
			}

			if (propName != null) {
				try {
					propName = Character.toLowerCase(propName.charAt(0))
							+ propName.substring(1);
					Object value = flattenParam(method.invoke(param));
					propsMap.put(propName, value);

				} catch (Exception e) {
					LOG.warn("Exception while accessing to prop '" + propName
							+ "' in param object: " + param, e);
				}
			}
		}

		return new Props(propsMap);
	}

	private boolean isPrimitive(Object param) {
		return param instanceof String || param instanceof Boolean
				|| param instanceof Integer || param instanceof Float;
	}

}
