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

import static com.kurento.kms.thrift.api.KmsMediaDataTypeConstants.BOOL_DATA_TYPE;
import static com.kurento.kms.thrift.api.KmsMediaDataTypeConstants.DOUBLE_DATA_TYPE;
import static com.kurento.kms.thrift.api.KmsMediaDataTypeConstants.I16_DATA_TYPE;
import static com.kurento.kms.thrift.api.KmsMediaDataTypeConstants.I32_DATA_TYPE;
import static com.kurento.kms.thrift.api.KmsMediaDataTypeConstants.I64_DATA_TYPE;
import static com.kurento.kms.thrift.api.KmsMediaDataTypeConstants.STRING_DATA_TYPE;
import static com.kurento.kms.thrift.api.KmsMediaDataTypeConstants.VOID_DATA_TYPE;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.kurento.kmf.media.params.MediaParam;
import com.kurento.kmf.media.params.internal.BooleanMediaParam;
import com.kurento.kmf.media.params.internal.DoubleMediaParam;
import com.kurento.kmf.media.params.internal.IntegerMediaParam;
import com.kurento.kmf.media.params.internal.LongMediaParam;
import com.kurento.kmf.media.params.internal.ShortMediaParam;
import com.kurento.kmf.media.params.internal.StringMediaParam;
import com.kurento.kmf.media.params.internal.VoidMediaParam;
import com.kurento.kms.thrift.api.KmsMediaParam;

//TODO put the correct payload in all params
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("/kmf-api-test-context.xml")
public class MediaParamTest {

	@Autowired
	private ApplicationContext ctx;

	@Test
	public void testBoolDataTypeInstantiation() {
		KmsMediaParam param = createKmsParam(BOOL_DATA_TYPE, null);
		instantiateAndCheck(BooleanMediaParam.class, param);
	}

	@Test
	public void testDoubleDataTypeInstantiation() {
		KmsMediaParam param = createKmsParam(DOUBLE_DATA_TYPE, null);
		instantiateAndCheck(DoubleMediaParam.class, param);
	}

	@Test
	public void testIntegerDataTypeInstantiation() {
		KmsMediaParam param = createKmsParam(I32_DATA_TYPE, null);
		instantiateAndCheck(IntegerMediaParam.class, param);
	}

	@Test
	public void testLongDataTypeInstantiation() {
		KmsMediaParam param = createKmsParam(I64_DATA_TYPE, null);
		instantiateAndCheck(LongMediaParam.class, param);
	}

	@Test
	public void testShorDataTypeInstantiation() {
		KmsMediaParam param = createKmsParam(I16_DATA_TYPE, null);
		instantiateAndCheck(ShortMediaParam.class, param);
	}

	@Test
	public void testStringDataTypeInstantiation() {
		KmsMediaParam param = createKmsParam(STRING_DATA_TYPE, null);
		instantiateAndCheck(StringMediaParam.class, param);
	}

	@Test
	public void testVoidDataTypeInstantiation() {
		KmsMediaParam param = createKmsParam(VOID_DATA_TYPE, null);
		instantiateAndCheck(VoidMediaParam.class, param);
	}

	private KmsMediaParam createKmsParam(String dataType, byte[] payload) {
		KmsMediaParam eventData = new KmsMediaParam();
		eventData.dataType = dataType;
		eventData.setData(payload);
		return eventData;
	}

	private void instantiateAndCheck(Class<?> expectedClass,
			KmsMediaParam kmsParam) {
		final MediaParam param = (MediaParam) ctx.getBean("mediaParam",
				kmsParam);
		Assert.assertEquals(param.getClass(), expectedClass);
	}

}
