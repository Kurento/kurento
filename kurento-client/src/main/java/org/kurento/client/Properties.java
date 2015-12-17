
package org.kurento.client;

import java.util.HashMap;
import java.util.Map;

public class Properties {

  private Map<String, Object> values = new HashMap<>();

  public static Properties of(Object... params) {

    if (params.length % 2 != 0) {
      throw new IllegalArgumentException(
          "Each key should have a value (pair number of parameters). Parameters are: " + params);
    }

    Properties p = new Properties();

    for (int i = 0; i < params.length; i += 2) {
      if (!(params[i] instanceof String)) {
        throw new IllegalArgumentException(
            "Property key should be an String value. Parameter " + i + " is " + params[i]);
      }
      p.add((String) params[i], params[i + 1]);
    }

    return p;
  }

  public Properties add(String property, Object value) {
    values.put(property, value);
    return this;
  }

  public Object get(String property) {
    return values.get(property);
  }

}
