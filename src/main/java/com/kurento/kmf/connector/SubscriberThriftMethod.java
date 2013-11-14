package com.kurento.kmf.connector;

import java.lang.reflect.Method;
import java.util.Arrays;

import org.apache.thrift.TFieldIdEnum;

public class SubscriberThriftMethod extends ThriftMethod {

	private String handlerAddress;
	private int handlerPort;

	public SubscriberThriftMethod(String jsonRpcMethodName, Method method,
			Class<?> argClass, TFieldIdEnum[] fields, String handlerAddress,
			int handlerPort) {
		super(jsonRpcMethodName, method, argClass,
				removeSubscribeParams(fields));
		this.handlerAddress = handlerAddress;
		this.handlerPort = handlerPort;
	}

	private static TFieldIdEnum[] removeSubscribeParams(TFieldIdEnum[] fields) {
		return Arrays.copyOf(fields, fields.length - 2);
	}

	@Override
	protected Object[] modifyParamsIfNeccessary(Object[] params) {

		Object[] newParams = Arrays.copyOf(params, params.length + 2);
		newParams[newParams.length - 2] = handlerAddress;
		newParams[newParams.length - 1] = handlerPort;
		return newParams;
	}
}
