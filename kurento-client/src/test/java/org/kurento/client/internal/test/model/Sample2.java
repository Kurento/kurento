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

import org.kurento.client.AbstractBuilder;
import org.kurento.client.KurentoObject;
import org.kurento.client.internal.client.RomManager;

public interface Sample2 extends KurentoObject {

  public String getAtt1();

  public int getAtt2();

  public float getAtt3();

  public boolean getAtt4();

  public static class Builder extends AbstractBuilder<Sample2> {

    public Builder(String att1, int att2, RomManager manager) {
      super(Sample2.class, manager);
      props.add("att1", att1);
      props.add("att2", att2);
    }

    public Builder withAtt3(float att3) {
      props.add("att3", att3);
      return this;
    }

    public Builder att4() {
      props.add("att4", Boolean.TRUE);
      return this;
    }
  }
}
