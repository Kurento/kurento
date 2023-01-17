/*
 * (C) Copyright 2016 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.kurento.client.internal.test;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.kurento.client.internal.test.model.client.ComplexParam;
import org.kurento.jsonrpc.JsonUtils;
import org.kurento.jsonrpc.Props;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;

public class RomJsonConverterTest {

  @Test
  public void stringConversion() {
    assertEquals(JsonUtils.fromJson(new JsonPrimitive("XXX"), String.class), "XXX");
  }

  @Test
  public void intConversion() {
    assertEquals((int) JsonUtils.fromJson(new JsonPrimitive(3), int.class), 3);
  }

  @Test
  public void floatConversion() {
    assertEquals(JsonUtils.fromJson(new JsonPrimitive(0.5), float.class), 0.5, 0.01);
  }

  @Test
  public void booleanConversion() {
    assertEquals((boolean) JsonUtils.fromJson(new JsonPrimitive(false), boolean.class), false);
  }

  @Test
  public void integerObjectConversion() {
    assertEquals(JsonUtils.fromJson(new JsonPrimitive(3), Integer.class), (Integer) 3);
  }

  @Test
  public void floatObjectConversion() {
    assertEquals(JsonUtils.fromJson(new JsonPrimitive(0.5), Float.class), 0.5, 0.01);
  }

  @Test
  public void booleanObjectConversion() {
    assertEquals(JsonUtils.fromJson(new JsonPrimitive(false), Boolean.class), false);
  }

  private static enum EnumType {
    CONST1, CONST2, CONST3
  }

  @Test
  public void stringToEnumConversion() {
    assertEquals(JsonUtils.fromJson(new JsonPrimitive("CONST1"), EnumType.class), EnumType.CONST1);
  }

  @Test
  public void enumToStringConversion() {
    assertEquals(JsonUtils.toJsonElement(EnumType.CONST1), new JsonPrimitive("CONST1"));
  }

  @Test
  public void stringListConversion() {

    JsonArray array = new JsonArray();
    array.add(new JsonPrimitive("XXX"));
    array.add(new JsonPrimitive("YYY"));
    array.add(new JsonPrimitive("ZZZ"));

    @SuppressWarnings("unchecked")
    List<String> list = JsonUtils.fromJson(array, List.class);

    assertEquals(list.get(0), "XXX");
    assertEquals(list.get(1), "YYY");
    assertEquals(list.get(2), "ZZZ");
  }

  @Test
  public void integerListConversion() {

    JsonArray array = new JsonArray();
    array.add(new JsonPrimitive(1));
    array.add(new JsonPrimitive(2));
    array.add(new JsonPrimitive(3));

    List<Integer> list = JsonUtils.fromJson(array, new TypeToken<List<Integer>>() {
    }.getType());

    assertEquals(list.get(0), (Integer) 1);
    assertEquals(list.get(1), (Integer) 2);
    assertEquals(list.get(2), (Integer) 3);
  }

  @Test
  public void floatListConversion() {

    JsonArray array = new JsonArray();
    array.add(new JsonPrimitive(0.1));
    array.add(new JsonPrimitive(0.2));
    array.add(new JsonPrimitive(0.3));

    List<Float> list = JsonUtils.fromJson(array, new TypeToken<List<Float>>() {
    }.getType());

    assertEquals(list.get(0), 0.1, 0.01);
    assertEquals(list.get(1), 0.2, 0.01);
    assertEquals(list.get(2), 0.3, 0.01);
  }

  @Test
  public void booleanListConversion() {

    JsonArray array = new JsonArray();
    array.add(new JsonPrimitive(true));
    array.add(new JsonPrimitive(false));
    array.add(new JsonPrimitive(true));

    @SuppressWarnings("unchecked")
    List<Boolean> list = JsonUtils.fromJson(array, List.class);

    assertEquals(list.get(0), true);
    assertEquals(list.get(1), false);
    assertEquals(list.get(2), true);
  }

  @Test
  public void stringToEnumListConversion() {

    JsonArray array = new JsonArray();
    array.add(new JsonPrimitive("CONST1"));
    array.add(new JsonPrimitive("CONST2"));
    array.add(new JsonPrimitive("CONST3"));

    List<EnumType> list = JsonUtils.fromJson(array, new TypeToken<List<EnumType>>() {
    }.getType());

    assertEquals(list.get(0), EnumType.CONST1);
    assertEquals(list.get(1), EnumType.CONST2);
    assertEquals(list.get(2), EnumType.CONST3);
  }

  @Test
  public void enumToStringListConversion() {

    List<EnumType> list = new ArrayList<EnumType>();
    list.add(EnumType.CONST1);
    list.add(EnumType.CONST2);
    list.add(EnumType.CONST3);

    JsonArray array = (JsonArray) JsonUtils.toJsonElement(list);

    assertEquals(array.get(0), new JsonPrimitive("CONST1"));
    assertEquals(array.get(1), new JsonPrimitive("CONST2"));
    assertEquals(array.get(2), new JsonPrimitive("CONST3"));
  }

  @Test
  public void objectToJsonConversion() {

    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("prop1", "XXX");
    jsonObject.addProperty("prop2", 33);
    jsonObject.addProperty("prop3", "YYY");
    jsonObject.addProperty("prop4", 5.5f);

    ComplexParam param = JsonUtils.fromJson(jsonObject, ComplexParam.class);

    assertEquals(param.getProp1(), "XXX");
    assertEquals(param.getProp2(), 33);
    assertEquals(param.getProp3(), "YYY");
    assertEquals(param.getProp4(), 5.5f, 0.01);
  }

  @Test
  public void jsonToObjectConversion() {

    ComplexParam param = new ComplexParam("XXX", 33);
    param.setProp3("YYY");
    param.setProp4(5.5f);

    JsonObject jsonObject = JsonUtils.toJsonObject(param);

    assertEquals(jsonObject.get("prop1").getAsString(), "XXX");
    assertEquals(jsonObject.get("prop2").getAsInt(), 33);
    assertEquals(jsonObject.get("prop3").getAsString(), "YYY");
    assertEquals(jsonObject.get("prop4").getAsFloat(), 5.5f, 0.01);

  }

  @Test
  public void objectListToJsonConversion() {

    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("prop1", "XXX");
    jsonObject.addProperty("prop2", 33);
    jsonObject.addProperty("prop3", "YYY");
    jsonObject.addProperty("prop4", 5.5f);

    JsonArray array = new JsonArray();
    array.add(jsonObject);

    JsonObject jsonObject2 = new JsonObject();
    jsonObject2.addProperty("prop1", "XXX2");
    jsonObject2.addProperty("prop2", 66);
    jsonObject2.addProperty("prop3", "YYY2");
    jsonObject2.addProperty("prop4", 11.5f);

    array.add(jsonObject2);

    List<ComplexParam> params = JsonUtils.fromJson(array, new TypeToken<List<ComplexParam>>() {
    }.getType());

    assertEquals(params.get(0).getProp1(), "XXX");
    assertEquals(params.get(0).getProp2(), 33);
    assertEquals(params.get(0).getProp3(), "YYY");
    assertEquals(params.get(0).getProp4(), 5.5f, 0.01);

    assertEquals(params.get(1).getProp1(), "XXX2");
    assertEquals(params.get(1).getProp2(), 66);
    assertEquals(params.get(1).getProp3(), "YYY2");
    assertEquals(params.get(1).getProp4(), 11.5f, 0.01);
  }

  @Test
  public void jsonToObjectListConversion() {

    ComplexParam param = new ComplexParam("XXX", 33);
    param.setProp3("YYY");
    param.setProp4(5.5f);

    ComplexParam param2 = new ComplexParam("XXX2", 66);
    param2.setProp3("YYY2");
    param2.setProp4(11.5f);

    List<ComplexParam> params = new ArrayList<>();
    params.add(param);
    params.add(param2);

    JsonArray jsonArray = (JsonArray) JsonUtils.toJsonElement(params);

    JsonObject obj0 = (JsonObject) jsonArray.get(0);
    assertEquals(obj0.get("prop1").getAsString(), "XXX");
    assertEquals(obj0.get("prop2").getAsInt(), 33);
    assertEquals(obj0.get("prop3").getAsString(), "YYY");
    assertEquals(obj0.get("prop4").getAsFloat(), 5.5f, 0.01);

    JsonObject obj1 = (JsonObject) jsonArray.get(1);
    assertEquals(obj1.get("prop1").getAsString(), "XXX2");
    assertEquals(obj1.get("prop2").getAsInt(), 66);
    assertEquals(obj1.get("prop3").getAsString(), "YYY2");
    assertEquals(obj1.get("prop4").getAsFloat(), 11.5f, 0.01);

  }

  @Test
  public void propsToJsonConversion() {

    JsonObject jsonObject = new JsonObject();
    jsonObject.addProperty("prop1", "XXX");
    jsonObject.addProperty("prop2", 33);
    jsonObject.addProperty("prop3", "YYY");
    jsonObject.addProperty("prop4", 5.5f);

    Props props = JsonUtils.fromJson(jsonObject, Props.class);

    assertEquals(props.getProp("prop1"), "XXX");
    assertEquals(props.getProp("prop2"), 33);
    assertEquals(props.getProp("prop3"), "YYY");
    assertEquals(props.getProp("prop4"), 5.5f);
  }

  @Test
  public void jsonToPropsConversion() {

    Props param = new Props();
    param.add("prop1", "XXX");
    param.add("prop2", 33);
    param.add("prop3", "YYY");
    param.add("prop4", 5.5f);

    JsonObject jsonObject = JsonUtils.toJsonObject(param);

    assertEquals(jsonObject.get("prop1").getAsString(), "XXX");
    assertEquals(jsonObject.get("prop2").getAsInt(), 33);
    assertEquals(jsonObject.get("prop3").getAsString(), "YYY");
    assertEquals(jsonObject.get("prop4").getAsFloat(), 5.5f, 0.01);

  }
}
