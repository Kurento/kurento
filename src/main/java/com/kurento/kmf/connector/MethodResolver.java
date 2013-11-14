package com.kurento.kmf.connector;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.thrift.TFieldIdEnum;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kms.thrift.api.KmsMediaServerService;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient;

public class MethodResolver {

	private Multimap<String, ThriftMethod> methods;

	private Multimap<String, String> overloadedMethods;

	private String handlerAddress;
	private int handlerPort;

	public MethodResolver(String handlerAddress, int handlerPort) {
		this.handlerAddress = handlerAddress;
		this.handlerPort = handlerPort;
		createMethodsMap();
	}

	private void createMethodsMap() {

		// Load thrift methods from AsyncClient class
		final Method[] classMethods = AsyncClient.class.getDeclaredMethods();
		Multimap<String, ThriftMethod> auxMethods = ArrayListMultimap.create();

		for (final Method method : classMethods) {

			Class<?> argsClass = getThriftMethodArgsClass(method.getName());

			final TFieldIdEnum[] fields = getThriftArgsFieldsClass(argsClass);

			ThriftMethod thriftMethod;
			if (method.getName().equals("subscribeError")
					|| method.getName().equals("subscribeEvent")) {

				thriftMethod = new SubscriberThriftMethod(method.getName(),
						method, argsClass, fields, handlerAddress, handlerPort);
			} else {
				thriftMethod = new ThriftMethod(method.getName(), method,
						argsClass, fields);
			}

			auxMethods.put(method.getName(), thriftMethod);
		}

		createOverloadedMethodsMap();

		// Modify method list to be "overloading" aware
		for (String overloadedMethod : overloadedMethods.keySet()) {

			List<ThriftMethod> realMethods = new ArrayList<ThriftMethod>();

			for (String realMethodName : overloadedMethods
					.get(overloadedMethod)) {
				realMethods.addAll(auxMethods.removeAll(realMethodName));
			}

			for (ThriftMethod realMethod : realMethods) {
				realMethod.setJsonRpcMethodName(overloadedMethod);
			}

			auxMethods.putAll(overloadedMethod, realMethods);
		}

		// Make immutable methods map
		this.methods = ImmutableMultimap.copyOf(auxMethods);
	}

	private void createOverloadedMethodsMap() {

		// Non overloaded methods:
		// getMediaElement
		// getConnectedSrc
		// getConnectedSinks
		// disconnect
		// connect
		// getMediaPipeline
		// getParent
		// invoke
		// release
		// keepAlive
		// getVersion
		// unsubscribeError
		// unsubscribeEvent

		com.google.common.collect.ImmutableMultimap.Builder<String, String> builder = new ImmutableMultimap.Builder<String, String>();

		builder.put("connectElements", "connectElements");
		builder.put("connectElements", "connectElementsByFullDescription");
		builder.put("connectElements", "connectElementsByMediaType");

		builder.put("getMediaSinks", "getMediaSinks");
		builder.put("getMediaSinks", "getMediaSinksByFullDescription");
		builder.put("getMediaSinks", "getMediaSinksByMediaType");

		builder.put("getMediaSrcs", "getMediaSrcs");
		builder.put("getMediaSrcs", "getMediaSrcsByFullDescription");
		builder.put("getMediaSrcs", "getMediaSrcsByMediaType");

		builder.put("createMediaMixer", "createMediaMixerWithParams");
		builder.put("createMediaMixer", "createMediaMixer");

		builder.put("createMediaElement", "createMediaElementWithParams");
		builder.put("createMediaElement", "createMediaElement");

		builder.put("createMediaPipeline", "createMediaPipelineWithParams");
		builder.put("createMediaPipeline", "createMediaPipeline");

		builder.put("createMixerEndPoint", "createMixerEndPointWithParams");
		builder.put("createMixerEndPoint", "createMixerEndPoint");

		overloadedMethods = builder.build();

	}

	public ThriftMethod lookup(String methodName, JsonObject params) {

		Collection<ThriftMethod> foundMethods = methods.get(methodName);

		if (foundMethods.size() == 0) {

			// TODO Error code
			throw new KurentoMediaFrameworkException(
					"There is no method with name '" + methodName
							+ "'. The supported methods are: "
							+ methods.keySet(), 444);

		} else {

			Set<String> paramNames = new HashSet<String>(getParamNames(params));

			if (foundMethods.size() == 1) {

				ThriftMethod method = foundMethods.iterator().next();

				if (!method.hasExactParameters(paramNames)) {
					
					for (ThriftMethod thriftMethod : foundMethods) {
						if (thriftMethod.hasAllParameters(paramNames)) {
							return thriftMethod;
						}
					}					
					
					throw new KurentoMediaFrameworkException("The method '"
							+ methodName + "' has parameters "
							+ method.getParamNames() + " but " + paramNames
							+ " are used to lookup", 444);
				}

				return method;

			} else {

				for (ThriftMethod thriftMethod : foundMethods) {
					if (thriftMethod.hasExactParameters(paramNames)) {
						return thriftMethod;
					}
				}
				
				for (ThriftMethod thriftMethod : foundMethods) {
					if (thriftMethod.hasAllParameters(paramNames)) {
						return thriftMethod;
					}
				}

				List<Set<String>> allParamNames = new ArrayList<Set<String>>();
				for (ThriftMethod thriftMethod : foundMethods) {
					allParamNames.add(thriftMethod.getParamNames());
				}

				// TODO Define exception with information.
				throw new KurentoMediaFrameworkException(
						"The method '"
								+ methodName
								+ "' has "
								+ foundMethods.size()
								+ " signatures (is overloaded). This signatures has the parameters: "
								+ allParamNames + " but " + paramNames
								+ " are used to lookup", 4444);
			}
		}
	}

	public List<String> getParamNames(JsonObject params) {

		if (params == null) {

			return Collections.emptyList();

		} else {

			List<String> names = new ArrayList<String>();

			for (Entry<String, JsonElement> entry : params.entrySet()) {
				names.add(entry.getKey());
			}

			return names;
		}
	}

	/**
	 * Obtains, from the thrift interface, the fields class from an args thrift
	 * object.
	 * 
	 * @param args
	 *            The args object
	 * @return The class containing the args to invoke the method
	 */
	@SuppressWarnings("rawtypes")
	private static TFieldIdEnum[] getThriftArgsFieldsClass(
			final Class<?> argsClass) {

		try {

			Class fieldsClass = Class.forName(argsClass.getName() + "$_Fields");
			return (TFieldIdEnum[]) fieldsClass.getEnumConstants();

		} catch (ClassNotFoundException e) {
			// TODO error code
			throw new KurentoMediaFrameworkException("Method fields "
					+ argsClass.getName() + " not found in current classloader");
		}
	}

	/**
	 * Obtains, from the thrift interface, the args class to invoke a certain
	 * method.
	 * 
	 * @param method
	 *            The thrift interface method name
	 * @return The class containing the args to invoke the method
	 */
	@SuppressWarnings("rawtypes")
	private static Class<?> getThriftMethodArgsClass(final String method) {

		final String argsClassName = KmsMediaServerService.class.getName()
				+ '$' + method + "_args";

		Class argClass;
		try {
			argClass = Class.forName(argsClassName);
		} catch (ClassNotFoundException e) {
			// TODO error code
			throw new KurentoMediaFrameworkException("Thrift args "
					+ argsClassName + " not found in current classloader");
		}
		return argClass;
	}

}
