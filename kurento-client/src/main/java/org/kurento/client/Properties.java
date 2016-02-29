
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
