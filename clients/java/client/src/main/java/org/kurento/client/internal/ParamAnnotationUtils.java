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

package org.kurento.client.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.kurento.client.internal.server.Param;
import org.kurento.client.internal.server.ProtocolException;
import org.kurento.jsonrpc.Props;

public class ParamAnnotationUtils {

  public static Props extractProps(List<String> paramNames, Object[] args) {
    Props props = null;

    if (!paramNames.isEmpty()) {
      props = new Props();
      for (int i = 0; i < args.length; i++) {
        props.add(paramNames.get(i), args[i]);
      }
    }
    return props;
  }

  public static Props extractProps(Annotation[][] annotations, Object[] args, int argsOffset) {
    Props props = null;

    if (args != null && args.length > 0) {

      props = new Props();
      for (int i = 0; i < args.length; i++) {

        Param param = getParamAnnotation(annotations[i + argsOffset]);
        props.add(param.value(), args[i]);
      }
    }

    return props;
  }

  public static Props extractProps(Annotation[][] annotations, Object[] args)
      throws ProtocolException {
    return extractProps(annotations, args, 0);
  }

  public static List<String> getParamNames(Method method) throws ProtocolException {
    return getParamNames(method.getParameterAnnotations());
  }

  public static List<String> getParamNames(Constructor<?> constructor) throws ProtocolException {
    return getParamNames(constructor.getParameterAnnotations());
  }

  public static List<String> getParamNames(Annotation[][] annotationsParams)
      throws ProtocolException {

    List<String> paramNames = new ArrayList<>();

    for (int x = 0; x < annotationsParams.length; x++) {
      Annotation[] annotationsParam = annotationsParams[x];
      Param paramAnnotation = getParamAnnotation(annotationsParam);
      if (paramAnnotation == null) {
        paramNames.add(null);
      } else {
        paramNames.add(paramAnnotation.value());
      }
    }

    return paramNames;
  }

  public static Param getParamAnnotation(Annotation[] annotationsParam) throws ProtocolException {

    Param param = null;

    for (int j = 0; j < annotationsParam.length; j++) {
      if (annotationsParam[j] instanceof Param) {
        param = (Param) annotationsParam[j];
        break;
      }
    }

    return param;
  }

  public static Object[] extractEventParams(Annotation[][] parameterAnnotations, Props data)
      throws ProtocolException {

    List<String> names = getParamNames(parameterAnnotations);

    Object[] params = new Object[names.size()];

    for (int i = 0; i < names.size(); i++) {
      String name = names.get(i);
      params[i] = data.getProp(name);
    }

    return params;
  }

}
