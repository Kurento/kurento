package org.kurento.client.internal.test.model.client;

import java.util.List;

import org.kurento.client.AbstractBuilder;
import org.kurento.client.Continuation;
import org.kurento.client.EventListener;
import org.kurento.client.KurentoObject;
import org.kurento.client.ListenerSubscription;
import org.kurento.client.internal.RemoteClass;
import org.kurento.client.internal.client.RomManager;
import org.kurento.client.internal.server.Param;
import org.kurento.client.internal.test.model.client.events.SampleEvent;

@RemoteClass
public interface SampleClass extends KurentoObject {

	String getAtt1();

	void getAtt1(Continuation<String> cont);

	boolean getAtt2();

	void getAtt2(Continuation<Boolean> cont);

	float getAtt3();

	void getAtt3(Continuation<Float> cont);

	int getAtt4();

	void getAtt4(Continuation<Integer> cont);

	void startTestEvents(@Param("numEvents") int numEvents);

	void startTestEvents(@Param("numEvents") int numEvents,
			Continuation<Void> cont);

	SampleEnum echoEnum(@Param("param") SampleEnum param);

	void echoEnum(@Param("param") SampleEnum param,
			Continuation<SampleEnum> cont);

	ComplexParam echoRegister(@Param("param") ComplexParam param);

	void echoRegister(@Param("param") ComplexParam param,
			Continuation<ComplexParam> cont);

	List<SampleEnum> echoListEnum(@Param("param") List<SampleEnum> param);

	void echoListEnum(@Param("param") List<SampleEnum> param,
			Continuation<List<SampleEnum>> cont);

	List<ComplexParam> echoListRegister(@Param("param") List<ComplexParam> param);

	void echoListRegister(@Param("param") List<ComplexParam> param,
			Continuation<List<ComplexParam>> cont);

	SampleClass echoObjectRef(@Param("param") SampleClass param);

	void echoObjectRef(@Param("param") SampleClass param,
			Continuation<SampleClass> cont);

	List<SampleClass> echoObjectRefList(@Param("param") List<SampleClass> param);

	void echoObjectRefList(@Param("param") List<SampleClass> param,
			Continuation<List<SampleClass>> cont);

	ListenerSubscription addSampleListener(EventListener<SampleEvent> listener);

	void addSampleListener(EventListener<SampleEvent> listener,
			Continuation<ListenerSubscription> cont);

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
	}
}
