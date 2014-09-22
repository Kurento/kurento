package org.kurento.client.internal.client.operation;

import java.lang.reflect.Type;

import org.kurento.client.Continuation;
import org.kurento.client.InternalInfoGetter;
import org.kurento.client.KurentoObject;
import org.kurento.client.internal.client.DefaultContinuation;
import org.kurento.client.internal.client.RomManager;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient;
import org.kurento.client.internal.transport.jsonrpc.RomClientJsonRpcClient.RequestAndResponseType;
import org.kurento.client.internal.transport.serialization.ParamsFlattener;
import org.kurento.jsonrpc.Props;

public class InvokeOperation extends Operation {

	private static ParamsFlattener FLATTENER = ParamsFlattener.getInstance();

	private KurentoObject object;
	private String method;
	private Props params;
	private Type returnType;

	public InvokeOperation(KurentoObject object, String method, Props params,
			Type returnType) {
		super();
		this.object = object;
		this.method = method;
		this.params = params;
		this.returnType = returnType;
	}

	@Override
	public void exec(RomManager manager) {

		Type flattenType = FLATTENER.calculateFlattenType(returnType);

		Object result = manager.invoke(
				InternalInfoGetter.getRemoteObject(object).getObjectRef(),
				method, params, flattenType);

		if (returnType != Void.class && returnType != void.class) {

			future.set(FLATTENER.unflattenValue("return", returnType, result,
					manager));
		}
	}

	@Override
	public void exec(final RomManager manager, final Continuation<Void> cont) {

		Type flattenType = FLATTENER.calculateFlattenType(returnType);

		manager.invoke(InternalInfoGetter.getRemoteObject(object)
				.getObjectRef(), method, params, flattenType,
				new DefaultContinuation<Object>(cont) {

					@Override
					public void onSuccess(Object result) throws Exception {
						if (returnType != Void.class
								&& returnType != void.class) {

							future.set(FLATTENER.unflattenValue("return",
									returnType, result, manager));
						}
						cont.onSuccess(null);
					}
				});
	}

	@Override
	public RequestAndResponseType createRequest(
			RomClientJsonRpcClient romClientJsonRpcClient) {

		Type flattenType = FLATTENER.calculateFlattenType(returnType);

		return romClientJsonRpcClient.createInvokeRequest(InternalInfoGetter
				.getRemoteObject(object).getObjectRef(), method, params,
				flattenType);
	}

	@Override
	public void processResponse(Object result) {

		if (returnType != Void.class && returnType != void.class) {

			future.set(FLATTENER.unflattenValue("return", returnType, result,
					manager));
		}
	}
}
