package org.kurento.client.internal.test.model;

import java.util.List;
import java.util.concurrent.Future;

import org.kurento.client.AbstractBuilder;
import org.kurento.client.KurentoObject;
import org.kurento.client.Transaction;
import org.kurento.client.internal.RemoteClass;
import org.kurento.client.internal.TransactionImpl;
import org.kurento.client.internal.client.RemoteObjectFacade;
import org.kurento.client.internal.client.RomManager;
import org.kurento.client.internal.client.operation.InvokeOperation;
import org.kurento.jsonrpc.Props;

import com.google.common.reflect.TypeToken;

@RemoteClass
public class SampleRemoteClass extends KurentoObject {

	public SampleRemoteClass(RemoteObjectFacade remoteObject) {
		super(remoteObject, null);
	}

	public void methodReturnVoid() {
		remoteObject.invoke("methodReturnVoid", null, Void.class);
	}

	public Future<Void> methodReturnVoid(Transaction t) {
		return (Future<Void>) ((TransactionImpl) t)
				.<Void> addOperation(new InvokeOperation(this,
						"methodReturnVoid", null, Void.class));
	}

	public String methodReturnsString() {
		return remoteObject.invoke("methodReturnsString", null, String.class);
	}

	public Future<String> methodReturnsString(Transaction t) {
		return (Future<String>) ((TransactionImpl) t)
				.<String> addOperation(new InvokeOperation(this,
						"methodReturnsString", null, Void.class));
	}

	public boolean methodReturnsBoolean() {
		return remoteObject.invoke("methodReturnsBoolean", null, boolean.class);
	}

	public float methodReturnsFloat() {
		return remoteObject.invoke("methodReturnsFloat", null, float.class);
	}

	public int methodReturnsInt() {
		return remoteObject.invoke("methodReturnsInt", null, int.class);
	}

	@SuppressWarnings({ "unchecked", "serial" })
	public List<String> methodReturnsStringList() {
		return (List<String>) remoteObject.invoke("methodReturnsStringList",
				null, new TypeToken<List<String>>() {
				}.getType());
	}

	@SuppressWarnings({ "unchecked", "serial" })
	public List<Boolean> methodReturnsBooleanList() {
		return (List<Boolean>) remoteObject.invoke("methodReturnsBooleanList",
				null, new TypeToken<List<Boolean>>() {
				}.getType());
	}

	@SuppressWarnings({ "unchecked", "serial" })
	public List<Float> methodReturnsFloatList() {
		return (List<Float>) remoteObject.invoke("methodReturnsFloatList",
				null, new TypeToken<List<Float>>() {
				}.getType());
	}

	@SuppressWarnings({ "unchecked", "serial" })
	public List<Integer> methodReturnsIntList() {
		return (List<Integer>) remoteObject.invoke("methodReturnsIntList",
				null, new TypeToken<List<Integer>>() {
				}.getType());
	}

	public String methodParamString(String param) {
		return remoteObject.invoke("methodParamString",
				new Props().add("param", param), String.class);
	}

	public boolean methodParamBoolean(boolean param) {
		return remoteObject.invoke("methodParamBoolean",
				new Props().add("param", param), boolean.class);
	}

	public float methodParamFloat(float param) {
		return remoteObject.invoke("methodParamFloat",
				new Props().add("param", param), float.class);
	}

	public int methodParamInt(int param) {
		return remoteObject.invoke("methodParamInt",
				new Props().add("param", param), int.class);
	}

	public static Builder with(RomManager manager) {
		return new Builder(manager);
	}

	public static class Builder extends AbstractBuilder<SampleRemoteClass> {

		public Builder(RomManager manager) {
			super(SampleRemoteClass.class, manager);
		}

		@Override
		protected SampleRemoteClass createMediaObject(
				RemoteObjectFacade remoteObject, Transaction tx) {
			return new SampleRemoteClass(remoteObject);
		}
	}
}
