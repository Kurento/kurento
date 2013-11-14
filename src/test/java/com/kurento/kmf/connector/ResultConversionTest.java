//package com.kurento.kmf.connector;
//
//import org.junit.Assert;
//import org.junit.Test;
//
//import com.kurento.kmf.jsonrpcconnector.JsonUtils;
//import com.kurento.kms.thrift.api.KmsMediaElement;
//import com.kurento.kms.thrift.api.KmsMediaObjectRef;
//import com.kurento.kms.thrift.api.KmsMediaObjectType;
//import com.kurento.kms.thrift.api.KmsMediaParam;
//
//public class ResultConversionTest {
//
//	@Test
//	public void testKmsMediaParam() {
//
//		KmsMediaParam result = new KmsMediaParam("V");
//
//		System.out.println("Before: " + JsonUtils.toJson(result));
//
//		Object resultObj = new AsyncCallbackProxy(null)
//				.convertToJsonRpcResponse(result);
//
//		String resultJson = JsonUtils.toJson(resultObj);
//
//		System.out.println("After: " + resultJson);
//
//		Assert.assertEquals("{\"data\":[\"DATA\"],\"type\":\"xxxxx\"}",
//				resultJson);
//	}
//
//	@Test
//	public void testKmsMediaObjectRef() {
//
//		KmsMediaObjectType objectType = new KmsMediaObjectType();
//		objectType.setElement(new KmsMediaElement("elementTypeValue"));
//		KmsMediaObjectRef result = new KmsMediaObjectRef(33, "xxxxx",
//				objectType);
//
//		System.out.println("Before: " + JsonUtils.toJson(result));
//
//		Object resultObj = new AsyncCallbackProxy(null)
//				.convertToJsonRpcResponse(result);
//
//		String resultJson = JsonUtils.toJson(resultObj);
//
//		System.out.println("After: " + resultJson);
//
//		Assert.assertEquals("{\"id\":33,\"token\":\"xxxxx\"}", resultJson);
//	}
//
//	@Test
//	public void testListOfKmsMediaObjectRef() {
//
//	}
// }
