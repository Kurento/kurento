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

import org.kurento.client.internal.RemoteClass;
import org.kurento.client.internal.server.Param;

@RemoteClass
public class Sample2Impl {

  private String att1;
  private int att2;
  private float att3;
  private boolean att4;

  public Sample2Impl(@Param("att1") String att1, @Param("att2") int att2, @Param("att3") float att3,
      @Param("att4") boolean att4) {
    this.att1 = att1;
    this.att2 = att2;
    this.att3 = att3;
    this.att4 = att4;
  }

  public String getAtt1() {
    return att1;
  }

  public int getAtt2() {
    return att2;
  }

  public float getAtt3() {
    return att3;
  }

  public boolean getAtt4() {
    return att4;
  }
}
