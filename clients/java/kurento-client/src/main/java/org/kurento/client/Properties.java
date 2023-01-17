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

package org.kurento.client;

import java.util.HashMap;
import java.util.Map;

public class Properties {

  private Map<String, Object> values;

  public static Properties of(Object... params) {

    if (params.length % 2 != 0) {
      throw new IllegalArgumentException(
          "Each key should have a value (pair number of parameters). Parameters are: " + params);
    }

    Properties props = new Properties();

    for (int i = 0; i < params.length; i += 2) {
      if (!(params[i] instanceof String)) {
        throw new IllegalArgumentException(
            "Property key should be an String value. Parameter " + i + " is " + params[i]);
      }
      props.add((String) params[i], params[i + 1]);
    }

    return props;
  }

  public Properties(Map<String, Object> values) {
    this.values = values;
  }

  public Properties() {
    this(new HashMap<String, Object>());
  }

  public Properties add(String property, Object value) {
    values.put(property, value);
    return this;
  }

  public Object get(String property) {
    return values.get(property);
  }

  public Map<String, Object> getMap() {
    return values;
  }
}
