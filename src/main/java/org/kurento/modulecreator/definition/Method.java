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
package org.kurento.modulecreator.definition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class Method extends NamedElement {

  private List<Param> params;

  @SerializedName("return")
  private Return returnProp;

  public Method(String name, String doc, List<Param> params, Return returnProp) {
    super(name, doc);
    this.setParams(params);

    this.returnProp = returnProp;
  }

  public List<Param> getParams() {
    return params;
  }

  public Return getReturn() {
    return returnProp;
  }

  public void setParams(List<Param> params) {
    this.params = params;
  }

  public void setReturnProp(Return returnProp) {
    this.returnProp = returnProp;
  }

  @Override
  public String toString() {
    return "Method [params=" + params + ", return=" + returnProp + ", doc=" + getDoc() + ", name="
        + getName() + "]";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((params == null) ? 0 : params.hashCode());
    result = prime * result + ((returnProp == null) ? 0 : returnProp.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!super.equals(obj)) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Method other = (Method) obj;
    if (params == null) {
      if (other.params != null) {
        return false;
      }
    } else if (!params.equals(other.params)) {
      return false;
    }
    if (returnProp == null) {
      if (other.returnProp != null) {
        return false;
      }
    } else if (!returnProp.equals(other.returnProp)) {
      return false;
    }
    return true;
  }

  @Override
  public List<ModelElement> getChildren() {
    List<ModelElement> children = new ArrayList<ModelElement>();

    if (params != null) {
      children.addAll(params);
    }

    if (returnProp != null) {
      children.add(returnProp);
    }

    return children;
  }

  public List<Method> expandIfOpsParams() {

    boolean optParam = false;
    for (Param param : this.params) {
      if (param.isOptional()) {
        optParam = true;
        break;
      }
    }

    if (optParam) {

      List<Method> expandedMethods = new ArrayList<Method>();
      for (Param param : this.params) {
        if (param.isOptional()) {

          List<Param> newParams = sublistUntilParam(params, param);
          expandedMethods.add(new Method(name, doc, newParams, returnProp));
        }
      }

      return expandedMethods;

    } else {
      return Collections.emptyList();
    }
  }

  private List<Param> sublistUntilParam(List<Param> params, Param centinelParam) {

    List<Param> newParams = new ArrayList<Param>();

    for (Param param : params) {
      if (param != centinelParam) {
        newParams.add(param);
      } else {
        break;
      }
    }
    return newParams;
  }

}
