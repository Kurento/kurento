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

package org.kurento.client.internal.test.model;

import java.util.Arrays;
import java.util.List;

import org.kurento.client.internal.RemoteClass;
import org.kurento.client.internal.server.Param;

@RemoteClass
public class SampleRemoteClassImpl {

  public void methodReturnVoid() {
  }

  public String methodReturnsString() {
    return "XXXX";
  }

  public boolean methodReturnsBoolean() {
    return false;
  }

  public float methodReturnsFloat() {
    return 0.5f;
  }

  public int methodReturnsInt() {
    return 0;
  }

  public List<String> methodReturnsStringList() {
    return Arrays.asList("XXXX");
  }

  public List<Boolean> methodReturnsBooleanList() {
    return Arrays.asList(false);
  }

  public List<Float> methodReturnsFloatList() {
    return Arrays.asList(0.5f);
  }

  public List<Integer> methodReturnsIntList() {
    return Arrays.asList(0);
  }

  public String methodParamString(@Param("param") String param) {
    return param;
  }

  public boolean methodParamBoolean(@Param("param") boolean param) {
    return param;
  }

  public float methodParamFloat(@Param("param") float param) {
    return param;
  }

  public int methodParamInt(@Param("param") int param) {
    return param;
  }

}
