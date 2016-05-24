
package org.kurento.commons;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import org.kurento.commons.exception.KurentoException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class PropertiesManager {

  public static interface PropertyHolder {
    public String getProperty(String property);
  }

  private static Gson gson;

  private static PropertyHolder propertyHolder = new PropertyHolder() {
    @Override
    public String getProperty(String property) {
      return System.getProperty(property);
    }
  };

  public static void setPropertyHolder(PropertyHolder propertyHolder) {
    PropertiesManager.propertyHolder = propertyHolder;
  }

  public static PropertyHolder getPropertyHolder() {
    return PropertiesManager.propertyHolder;
  }

  public static String getPropertyOrException(String property, String exceptionMessage) {

    String value = getProperty(property);

    if (value == null) {
      throw new KurentoException(exceptionMessage);
    }

    return value;
  }

  public static String getProperty(String property) {
    return propertyHolder.getProperty(property);
  }

  public static Address getProperty(String property, Address defaultValue) {

    String systemValue = propertyHolder.getProperty(property);

    if (systemValue == null) {
      return defaultValue;
    } else {
      String[] parts = systemValue.split(":");
      if (parts.length == 0) {
        return defaultValue;
      } else if (parts.length == 1) {
        return new Address(parts[0], defaultValue.getPort());
      } else {
        String host = parts[0];
        int port;
        try {
          port = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
          port = defaultValue.getPort();
        }
        return new Address(host, port);
      }
    }
  }

  public static int getProperty(String property, int defaultValue) {
    String systemValue = propertyHolder.getProperty(property);
    return systemValue != null ? Integer.parseInt(systemValue) : defaultValue;
  }

  public static double getProperty(String property, double defaultValue) {
    String systemValue = propertyHolder.getProperty(property);
    return systemValue != null ? Double.parseDouble(systemValue) : defaultValue;
  }

  public static long getProperty(String property, long defaultValue) {
    String systemValue = propertyHolder.getProperty(property);
    return systemValue != null ? Integer.parseInt(systemValue) : defaultValue;
  }

  public static String getProperty(String property, String defaultValue) {
    String value = propertyHolder.getProperty(property);
    if (value != null) {
      return value;
    } else {
      return defaultValue;
    }
  }

  @SuppressWarnings("unchecked")
  public static <E extends Enum<E>> E getProperty(String property, E defaultValue) {
    String value = propertyHolder.getProperty(property);
    if (value != null) {
      return Enum.valueOf((Class<E>) defaultValue.getClass(), value.toUpperCase());
    } else {
      return defaultValue;
    }
  }

  public static boolean getProperty(String property, boolean defaultValue) {
    String systemValue = propertyHolder.getProperty(property);
    return systemValue != null ? Boolean.parseBoolean(systemValue) : defaultValue;
  }

  public static <T extends JsonElement> T getPropertyJson(String property, String defaultValue,
      Class<T> clazz) {
    String value = getProperty(property, defaultValue);
    initGson();
    return gson.fromJson(value, clazz);
  }

  public static <T> T getPropertyJson(String property, String defaultValue, Type classOfT) {
    String value = getProperty(property, defaultValue);
    initGson();
    return gson.fromJson(value, classOfT);
  }

  private static void initGson() {
    if (gson == null) {
      synchronized (PropertiesManager.class) {
        if (gson == null) {
          gson = new GsonBuilder().create();
        }
      }
    }
  }

  public static Map<String, Object> getProperty(String property, Map<String, Object> defaultMap) {

    Map<String, Object> result = new HashMap<>(defaultMap);

    Map<String, Object> propertiesMap = BasicJsonUtils
        .toPropertiesMap(getPropertyJson(property, "{}", JsonObject.class));

    result.putAll(propertiesMap);

    return result;
  }
}