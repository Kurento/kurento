//package com.kurento.kmf.jsonrpcconnector.client;
//
//import java.io.IOException;
//
//import org.apache.thrift.async.AsyncMethodCallback;
//
//import com.kurento.kmf.jsonrpcconnector.internal.JsonRpcRequestSenderHelper;
//import com.kurento.kmf.jsonrpcconnector.internal.message.Request;
//import com.kurento.kmf.jsonrpcconnector.internal.message.Response;
//import com.kurento.kmf.thrift.pool.MediaServerClientPoolService;
//import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient;
//import com.kurento.kms.thrift.api.KmsMediaServerService.AsyncClient.invokeJsonRpc_call;
//
//public class JsonRpcClientThrift extends JsonRpcClient {
//
//	private MediaServerClientPoolService clientPool;
//
//	public JsonRpcClientThrift(MediaServerClientPoolService clientPool) {
//		
//		this.clientPool = clientPool;
//		
//		this.rsHelper = new JsonRpcRequestSenderHelper() {
//			@Override
//			public <P, R> Response<R> internalSendRequest(Request<P> request,
//					Class<R> resultClass) throws IOException {
//				return internalSendRequestThrift(request, resultClass);
//			}
//		};
//	}
//	
//	public <P, R> Response<R> internalSendRequestThrift(Request<P> request,
//			Class<R> resultClass) {
//		
//		AsyncClient client = clientPool.acquireAsync();
//
//		transaction.startAsync();
//		
//		if(request.getMethod().equals("subscribe")) {
//			request.getParams().addProperty("ip", config.getHandlerAddress());
//			request.getParams().addProperty("port", config.getHandlerPort());
//		}
//		
//		client.invokeJsonRpc(request.toString(),
//				new AsyncMethodCallback<invokeJsonRpc_call>() {
//
//					@Override
//					public void onComplete(invokeJsonRpc_call response) {
//						requestOnComplete(response, transaction);
//					}
//
//					@Override
//					public void onError(Exception exception) {
//						requestOnError(exception, transaction);
//					}
//				});
//		
//	}
//	
//	@Override
//	public void close() throws IOException {
//				
//	}
//	
//	
//
// }
