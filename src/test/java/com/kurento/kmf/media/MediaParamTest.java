/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package com.kurento.kmf.media;

import static com.kurento.kmf.media.Utils.createKmsParam;
import static com.kurento.kms.thrift.api.KmsMediaDataTypeConstants.BOOL_DATA_TYPE;
import static com.kurento.kms.thrift.api.KmsMediaDataTypeConstants.BYTE_DATA_TYPE;
import static com.kurento.kms.thrift.api.KmsMediaDataTypeConstants.DOUBLE_DATA_TYPE;
import static com.kurento.kms.thrift.api.KmsMediaDataTypeConstants.I16_DATA_TYPE;
import static com.kurento.kms.thrift.api.KmsMediaDataTypeConstants.I32_DATA_TYPE;
import static com.kurento.kms.thrift.api.KmsMediaDataTypeConstants.I64_DATA_TYPE;
import static com.kurento.kms.thrift.api.KmsMediaDataTypeConstants.STRING_DATA_TYPE;
import static com.kurento.kms.thrift.api.KmsMediaDataTypeConstants.VOID_DATA_TYPE;
import static com.kurento.kms.thrift.api.KmsMediaZBarFilterTypeConstants.EVENT_CODE_FOUND_DATA_TYPE;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.kmf.media.params.MediaParam;
import com.kurento.kmf.media.params.internal.BooleanMediaParam;
import com.kurento.kmf.media.params.internal.ByteMediaParam;
import com.kurento.kmf.media.params.internal.DefaultMediaParam;
import com.kurento.kmf.media.params.internal.DoubleMediaParam;
import com.kurento.kmf.media.params.internal.EventCodeFoundParam;
import com.kurento.kmf.media.params.internal.HttpEndpointConstructorParam;
import com.kurento.kmf.media.params.internal.IntegerMediaParam;
import com.kurento.kmf.media.params.internal.LongMediaParam;
import com.kurento.kmf.media.params.internal.MediaObjectConstructorParam;
import com.kurento.kmf.media.params.internal.PointerDetectorConstructorParam;
import com.kurento.kmf.media.params.internal.PointerDetectorWindowMediaParam;
import com.kurento.kmf.media.params.internal.RecorderEndpointConstructorParam;
import com.kurento.kmf.media.params.internal.ShortMediaParam;
import com.kurento.kmf.media.params.internal.StringMediaParam;
import com.kurento.kmf.media.params.internal.UriEndpointConstructorParam;
import com.kurento.kmf.media.params.internal.VoidMediaParam;
import com.kurento.kms.thrift.api.KmsMediaHttpEndPointTypeConstants;
import com.kurento.kms.thrift.api.KmsMediaMuxer;
import com.kurento.kms.thrift.api.KmsMediaObjectConstants;
import com.kurento.kms.thrift.api.KmsMediaParam;
import com.kurento.kms.thrift.api.KmsMediaPointerDetectorFilterTypeConstants;
import com.kurento.kms.thrift.api.KmsMediaRecorderEndPointTypeConstants;
import com.kurento.kms.thrift.api.KmsMediaUriEndPointTypeConstants;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/kmf-api-test-context.xml")
public class MediaParamTest {

	private static final String DEFAULT_PARAM = "default media param";

	@Autowired
	private ApplicationContext ctx;

	@Test
	public void testDefaultMediaParam() {
		DefaultMediaParam in = new DefaultMediaParam(DEFAULT_PARAM);
		String str = "string to use as payload";
		in.setData(str.getBytes());
		// This will get the payload serialised
		KmsMediaParam param = createKmsParam(DEFAULT_PARAM, in
				.getThriftParams().getData());
		DefaultMediaParam out = instantiateAndCheck(DefaultMediaParam.class,
				param);
		// Check if what was serialised is the same as what was received.
		String outStr = new String(out.getData());
		Assert.assertEquals(str, outStr);
	}

	@Test
	public void testBoolDataTypeInstantiation() {
		BooleanMediaParam in = new BooleanMediaParam();
		in.setBoolean(true);
		// This will get the payload serialised
		KmsMediaParam param = createKmsParam(BOOL_DATA_TYPE, in
				.getThriftParams().getData());
		BooleanMediaParam out = instantiateAndCheck(BooleanMediaParam.class,
				param);
		// Check if what was serialised is the same as what was received.
		Assert.assertTrue(out.getBoolean() == in.getBoolean());
	}

	@Test
	public void testByteDataTypeInstantiation() {
		ByteMediaParam in = new ByteMediaParam();
		in.setByte((byte) 100);
		// This will get the payload serialised
		KmsMediaParam param = createKmsParam(BYTE_DATA_TYPE, in
				.getThriftParams().getData());
		ByteMediaParam out = instantiateAndCheck(ByteMediaParam.class, param);
		// Check if what was serialised is the same as what was received.
		Assert.assertTrue(out.getByte() == in.getByte());
	}

	@Test
	public void testDoubleDataTypeInstantiation() {
		DoubleMediaParam in = new DoubleMediaParam();
		in.setDouble(123.123);
		KmsMediaParam param = createKmsParam(DOUBLE_DATA_TYPE, in
				.getThriftParams().getData());

		DoubleMediaParam out = instantiateAndCheck(DoubleMediaParam.class,
				param);
		// Check if what was serialised is the same as what was received.
		Assert.assertTrue(out.getDouble() == in.getDouble());
	}

	@Test
	public void testEventCodeFoundInstantiation() {
		EventCodeFoundParam in = new EventCodeFoundParam();
		in.setCodeType("A code");
		in.setValue("A value");
		KmsMediaParam param = createKmsParam(EVENT_CODE_FOUND_DATA_TYPE, in
				.getThriftParams().getData());

		EventCodeFoundParam out = instantiateAndCheck(
				EventCodeFoundParam.class, param);
		// Check if what was serialised is the same as what was received.
		Assert.assertEquals(in.getCodeType(), out.getCodeType());
		Assert.assertEquals(in.getValue(), out.getValue());
	}

	@Test
	public void testIntegerDataTypeInstantiation() {
		IntegerMediaParam in = new IntegerMediaParam();
		in.setInteger(123);
		KmsMediaParam param = createKmsParam(I32_DATA_TYPE, in
				.getThriftParams().getData());

		IntegerMediaParam out = instantiateAndCheck(IntegerMediaParam.class,
				param);
		// Check if what was serialised is the same as what was received.
		Assert.assertTrue(out.getInteger() == in.getInteger());
	}

	@Test
	public void testLongDataTypeInstantiation() {
		LongMediaParam in = new LongMediaParam();
		in.setLong(123);
		KmsMediaParam param = createKmsParam(I64_DATA_TYPE, in
				.getThriftParams().getData());

		LongMediaParam out = instantiateAndCheck(LongMediaParam.class, param);
		// Check if what was serialised is the same as what was received.
		Assert.assertTrue(out.getLong() == in.getLong());
	}

	@Test
	public void testShorDataTypeInstantiation() {
		ShortMediaParam in = new ShortMediaParam();
		in.setShort((short) 123);
		KmsMediaParam param = createKmsParam(I16_DATA_TYPE, in
				.getThriftParams().getData());

		ShortMediaParam out = instantiateAndCheck(ShortMediaParam.class, param);
		// Check if what was serialised is the same as what was received.
		Assert.assertTrue(out.getShort() == in.getShort());
	}

	@Test
	public void testStringDataTypeInstantiation() {
		StringMediaParam in = new StringMediaParam();
		in.setString("A string");
		KmsMediaParam param = createKmsParam(STRING_DATA_TYPE, in
				.getThriftParams().getData());

		StringMediaParam out = instantiateAndCheck(StringMediaParam.class,
				param);
		// Check if what was serialised is the same as what was received.
		Assert.assertEquals(out.getString(), in.getString());
	}

	@Test
	public void testVoidDataTypeInstantiation() {
		VoidMediaParam in = new VoidMediaParam();
		KmsMediaParam param = createKmsParam(VOID_DATA_TYPE, in
				.getThriftParams().getData());

		VoidMediaParam out = instantiateAndCheck(VoidMediaParam.class, param);
		// This object has no params
		Assert.assertTrue(out.getThriftParams().getData().length == 0);
	}

	@Test
	public void testUriParamConstructor() {
		UriEndpointConstructorParam in = new UriEndpointConstructorParam();
		in.setUri("http://test.com");
		KmsMediaParam param = createKmsParam(
				KmsMediaUriEndPointTypeConstants.CONSTRUCTOR_PARAMS_DATA_TYPE,
				in.getThriftParams().getData());

		UriEndpointConstructorParam out = instantiateAndCheck(
				UriEndpointConstructorParam.class, param);
		Assert.assertTrue(in.getUri().equals(out.getUri()));
	}

	@Test
	public void testMediaObjectConstructor() {
		MediaObjectConstructorParam in = new MediaObjectConstructorParam();
		in.setGarbageCollectorPeriod(100);
		KmsMediaParam param = createKmsParam(
				KmsMediaObjectConstants.CONSTRUCTOR_PARAMS_DATA_TYPE, in
						.getThriftParams().getData());

		MediaObjectConstructorParam out = instantiateAndCheck(
				MediaObjectConstructorParam.class, param);
		Assert.assertTrue(in.getGarbageCollectorPeriod() == out
				.getGarbageCollectorPeriod());
	}

	@Test
	public void testHttpEndpointConstructor() {
		HttpEndpointConstructorParam in = new HttpEndpointConstructorParam();
		in.setDisconnectionTimeout(Integer.valueOf(200));
		in.setMediaMuxer(KmsMediaMuxer.MP4);
		in.setTerminateOnEOS(Boolean.FALSE);
		KmsMediaParam param = createKmsParam(
				KmsMediaHttpEndPointTypeConstants.CONSTRUCTOR_PARAMS_DATA_TYPE,
				in.getThriftParams().getData());

		HttpEndpointConstructorParam out = instantiateAndCheck(
				HttpEndpointConstructorParam.class, param);
		Assert.assertTrue(in.getDisconnectionTimeout().equals(
				out.getDisconnectionTimeout()));
		Assert.assertTrue(in.getMediaMuxer().equals(out.getMediaMuxer()));
		Assert.assertTrue(in.getTerminateOnEOS()
				.equals(out.getTerminateOnEOS()));
	}

	@Test
	public void testRecoderEndpointConstructor() {
		RecorderEndpointConstructorParam in = new RecorderEndpointConstructorParam();
		in.setMediaMuxer(KmsMediaMuxer.MP4);
		KmsMediaParam param = createKmsParam(
				KmsMediaRecorderEndPointTypeConstants.CONSTRUCTOR_PARAMS_DATA_TYPE,
				in.getThriftParams().getData());

		RecorderEndpointConstructorParam out = instantiateAndCheck(
				RecorderEndpointConstructorParam.class, param);
		Assert.assertTrue(in.getMediaMuxer().equals(out.getMediaMuxer()));
	}

	@Test
	public void testPointerDetectorWindowMediaParam() {
		PointerDetectorWindowMediaParam in = new PointerDetectorWindowMediaParam(
				"id", 1, 2, 3, 4);

		KmsMediaParam param = createKmsParam(
				KmsMediaPointerDetectorFilterTypeConstants.ADD_NEW_WINDOW_PARAM_WINDOW,
				in.getThriftParams().getData());

		PointerDetectorWindowMediaParam out = instantiateAndCheck(
				PointerDetectorWindowMediaParam.class, param);
		Assert.assertEquals(in, out);
	}

	@Test
	public void testPointerDetectorConstructorParam() {
		PointerDetectorConstructorParam in = new PointerDetectorConstructorParam();
		in.addDetectorWindow("id", 1, 2, 3, 4);
		KmsMediaParam param = createKmsParam(
				KmsMediaPointerDetectorFilterTypeConstants.CONSTRUCTOR_PARAMS_DATA_TYPE,
				in.getThriftParams().getData());

		PointerDetectorConstructorParam out = instantiateAndCheck(
				PointerDetectorConstructorParam.class, param);
		Assert.assertEquals(in, out);
	}

	private <T extends MediaParam> T instantiateAndCheck(
			Class<T> expectedClass, KmsMediaParam kmsParam) {
		final MediaParam param = (MediaParam) ctx.getBean("mediaParam",
				kmsParam);
		Assert.assertEquals(param.getClass(), expectedClass);
		return (T) param;
	}

}
