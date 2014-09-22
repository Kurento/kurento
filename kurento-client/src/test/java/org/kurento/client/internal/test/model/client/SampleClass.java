package org.kurento.client.internal.test.model.client;

import java.util.List;

import org.kurento.client.AbstractBuilder;
import org.kurento.client.EventListener;
import org.kurento.client.KurentoObject;
import org.kurento.client.ListenerSubscription;
import org.kurento.client.Transaction;
import org.kurento.client.internal.RemoteClass;
import org.kurento.client.internal.client.RemoteObjectFacade;
import org.kurento.client.internal.client.RomManager;
import org.kurento.client.internal.test.model.client.events.SampleEvent;
import org.kurento.jsonrpc.Props;

import com.google.common.reflect.TypeToken;

@RemoteClass
public class SampleClass extends KurentoObject {

	public SampleClass(RemoteObjectFacade remoteObject) {
		super(remoteObject, null);
	}

	public String getAtt1() {
		return (String) remoteObject.invoke("getAtt1", null, String.class);
	}

	public boolean getAtt2() {
		return (boolean) remoteObject.invoke("getAtt2", null, boolean.class);
	}

	public float getAtt3() {
		return (float) remoteObject.invoke("getAtt3", null, float.class);
	}

	public int getAtt4() {
		return (int) remoteObject.invoke("getAtt4", null, int.class);
	}

	public void startTestEvents(int numEvents) {
		Props props = new Props();
		props.add("numEvents", numEvents);
		remoteObject.invoke("startTestEvents", props, Void.class);
	}

	public SampleEnum echoEnum(SampleEnum param) {
		Props props = new Props();
		props.add("param", param);
		return (SampleEnum) remoteObject.invoke("echoEnum", props,
				SampleEnum.class);
	}

	public ComplexParam echoRegister(ComplexParam param) {
		Props props = new Props();
		props.add("param", param);
		return (ComplexParam) remoteObject.invoke("echoRegister", props,
				ComplexParam.class);
	}

	@SuppressWarnings({ "unchecked", "serial" })
	public List<SampleEnum> echoListEnum(List<SampleEnum> param) {
		Props props = new Props();
		props.add("param", param);
		return (List<SampleEnum>) remoteObject.invoke("echoListEnum", props,
				new TypeToken<List<SampleEnum>>() {
				}.getType());
	}

	// @SuppressWarnings({ "serial" })
	// public void echoListEnum(List<SampleEnum> param,
	// Continuation<List<SampleEnum>> cont) {
	// Props props = new Props();
	// props.add("param", param);
	// remoteObject.invoke("echoListEnum", props,
	// new TypeToken<List<SampleEnum>>() {
	// }.getType(), cont);
	// }

	@SuppressWarnings({ "unchecked", "serial" })
	public List<ComplexParam> echoListRegister(List<ComplexParam> param) {
		Props props = new Props();
		props.add("param", param);
		return (List<ComplexParam>) remoteObject.invoke("echoListRegister",
				props, new TypeToken<List<ComplexParam>>() {
				}.getType());
	}

	// @SuppressWarnings({ "serial" })
	// public void echoListRegister(List<ComplexParam> param,
	// Continuation<List<ComplexParam>> cont) {
	// Props props = new Props();
	// props.add("param", param);
	// remoteObject.invoke("echoListRegister", props,
	// new TypeToken<List<ComplexParam>>() {
	// }.getType(), cont);
	// }

	public SampleClass echoObjectRef(SampleClass param) {
		Props props = new Props();
		props.add("param", param);
		return (SampleClass) remoteObject.invoke("echoObjectRef", props,
				SampleClass.class);
	}

	// public void echoObjectRef(SampleClass param, Continuation<SampleClass>
	// cont) {
	// Props props = new Props();
	// props.add("param", param);
	// remoteObject.invoke("echoObjectRef", props, SampleClass.class, cont);
	// }

	@SuppressWarnings({ "unchecked", "serial" })
	public List<SampleClass> echoObjectRefList(List<SampleClass> param) {
		Props props = new Props();
		props.add("param", param);
		return (List<SampleClass>) remoteObject.invoke("echoObjectRefList",
				props, new TypeToken<List<SampleClass>>() {
				}.getType());
	}

	public ListenerSubscription addSampleListener(
			EventListener<SampleEvent> listener) {
		return subscribeEventListener(listener, SampleEvent.class);
	}

	public static Builder with(String att1, boolean att2, RomManager manager) {
		return new Builder(att1, att2, manager);
	}

	public static class Builder extends AbstractBuilder<SampleClass> {

		public Builder(String att1, boolean att2, RomManager manager) {
			super(SampleClass.class, manager);
			props.add("att1", att1);
			props.add("att2", att2);
		}

		public Builder withAtt3(float att3) {
			props.add("att3", att3);
			return this;
		}

		public Builder withAtt4(int att4) {
			props.add("att4", att4);
			return this;
		}

		@Override
		protected SampleClass createMediaObject(
				RemoteObjectFacade remoteObject, Transaction tx) {
			return new SampleClass(remoteObject);
		}
	}
}
