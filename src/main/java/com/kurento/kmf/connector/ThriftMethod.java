package com.kurento.kmf.connector;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.thrift.TBase;
import org.apache.thrift.TBaseHelper;
import org.apache.thrift.TException;
import org.apache.thrift.TFieldIdEnum;
import org.apache.thrift.async.AsyncMethodCallback;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.transport.TMemoryBuffer;

import com.google.common.collect.Multiset.Entry;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import com.kurento.kmf.common.exception.KurentoMediaFrameworkException;
import com.kurento.kmf.jsonrpcconnector.JsonUtils;
import com.kurento.kmf.jsonrpcconnector.Transaction;
import com.kurento.kms.thrift.api.KmsMediaObjectRef;
import com.kurento.kms.thrift.api.KmsMediaParam;
import com.kurento.kms.thrift.api.KmsMediaServerService;
import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient;
import com.kurento.kms.thrift.api.KmsMediaServerService.createMediaElementWithParams_args;
import com.kurento.kms.thrift.api.KmsMediaServerService.createMediaElement_args;
import com.kurento.kms.thrift.api.KmsMediaUriEndPointConstructorParams;
import com.kurento.kms.thrift.api.KmsMediaUriEndPointTypeConstants;

public class ThriftMethod {

	private Class<?> argClass;
	final TFieldIdEnum[] fields;
	private Method method;
	private String jsonRpcMethodName;
	private Set<String> paramNames;

	public ThriftMethod(String jsonRpcMethodName, Method method,
			Class<?> argClass, TFieldIdEnum[] fields) {
		this.argClass = argClass;
		this.fields = fields;
		this.method = method;
		this.jsonRpcMethodName = jsonRpcMethodName;

		this.paramNames = new HashSet<String>();
		for (int i = 0; i < fields.length; i++) {
			paramNames.add(fields[i].getFieldName());
		}
	}

	protected Object[] modifyParamsIfNeccessary(Object[] params) {
		return params;
	}

	public void invoke(AsyncClient client, JsonObject params,
			Transaction transaction) throws InvocationTargetException {

		Object[] methodParams = parseParamsFromRequest(params);

		methodParams = addCallbackToParams(methodParams, transaction);

		try {
			method.invoke(client, methodParams);
		} catch (IllegalAccessException e) {
			// TODO What to do here?
			throw new KurentoMediaFrameworkException(
					"An exception occurred in the Media Server invoking the method "
							+ jsonRpcMethodName);
		}
	}

	/**
	 * Obtains the array of parameters for an asynchronous method call from the
	 * Thrift interface. This method reflectively finds the proper classes,
	 * deserialising the JsonObject stored in the request to the appropriate
	 * field form the thrift interface.
	 * 
	 * This method creates a Dynamic Proxy, of the type
	 * {@link AsyncCallbackProxy} to be used as callback for an asynchronous
	 * request.
	 * 
	 * @param request
	 *            The full request deserialised.
	 * @param tx
	 *            The transaction used by the dynamic proxy to send the
	 *            responses from the KMS
	 * @return The parameters needed to execute the command in
	 *         {@link JsonRpcRequest#method}. To the basic list of parameters,
	 *         the callback needed to make the asynchronous call is added.
	 */
	private Object[] addCallbackToParams(Object[] params, Transaction tx) {

		final AsyncMethodCallback<?> callback = new AsyncCallbackProxy(tx);

		params = Arrays.copyOf(params, params.length + 1);
		params[params.length - 1] = callback;

		return params;
	}

	protected byte[] getByteArray(TBase kmsData) {
		TMemoryBuffer tr = new TMemoryBuffer(512);
		TProtocol pr = new TBinaryProtocol(tr);
		try {
			kmsData.write(pr);
		} catch (TException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Arrays.copyOf(tr.getArray(), tr.length());
	}

	@SuppressWarnings("unchecked")
	public Object[] parseParamsFromRequest(JsonObject params) {

		try {

			if (jsonRpcMethodName.equals("createMediaElement")
					&& params.get("type").getAsString()
							.equals("PlayerEndPoint")) {

				Map<String, KmsMediaParam> kmsMediaParams = new HashMap<String, KmsMediaParam>();

				KmsMediaUriEndPointConstructorParams kmuecp = new KmsMediaUriEndPointConstructorParams();
				kmuecp.setUri("https://ci.kurento.com/video/fiwarecut.webm");
				KmsMediaParam param = new KmsMediaParam();
				param.dataType = KmsMediaUriEndPointTypeConstants.CONSTRUCTOR_PARAMS_DATA_TYPE;
				param.setData(getByteArray(param));

				kmsMediaParams
						.put(KmsMediaUriEndPointTypeConstants.CONSTRUCTOR_PARAMS_DATA_TYPE,
								param);

//				createMediaElementWithParams_args constParams = new KmsMediaServerService.createMediaElementWithParams_args();
//				constParams.setType("PlayerEndPoint");
//				constParams.setPipeline();
//				constParams.setParams();
				
				return new Object[] { JsonUtils.fromJson(params.get("pipeline"), KmsMediaObjectRef.class), "PlayerEndPoint", kmsMediaParams };

			} else {

				final TBase<?, TFieldIdEnum> thriftParamsObj = (TBase<?, TFieldIdEnum>) JsonUtils
						.fromJson(params, argClass);

				final List<Object> params1;

				if (thriftParamsObj == null) {
					params1 = Collections.emptyList();

				} else {
					params1 = new ArrayList<Object>(fields.length);
					for (TFieldIdEnum field : fields) {
						// argClass.getField(field.getFieldName());
						Object param = thriftParamsObj.getFieldValue(field);
						params1.add(param);
					}
				}

				return params1.toArray();
			}

		} catch (JsonSyntaxException e) {
			// TODO Error code
			throw new KurentoMediaFrameworkException(
					"Exception when parsing json params: '" + params
							+ "' into class " + argClass.getCanonicalName(), e,
					444);
		}
	}

	public boolean hasExactParameters(Set<String> paramNames) {
		return this.paramNames.equals(paramNames);
	}

	public boolean hasAllParameters(Set<String> paramNames) {
		return this.paramNames.containsAll(paramNames);
	}

	public Set<String> getParamNames() {
		return paramNames;
	}

	public void setJsonRpcMethodName(String jsonRpcMethodName) {
		this.jsonRpcMethodName = jsonRpcMethodName;
	}

}
