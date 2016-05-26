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

public class NamedElement extends ModelElement {

  protected String name;
  protected String doc;

  public NamedElement(String name, String doc) {
    super();
    this.name = name;
    this.doc = doc;
  }

  public String getDoc() {
    return doc;
  }

  public String getName() {
    return name;
  }

  public void setDoc(String doc) {
    this.doc = doc;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void updateName(String name) {
    this.name = name;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((doc == null) ? 0 : doc.hashCode());
    result = prime * result + ((name == null) ? 0 : name.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    NamedElement other = (NamedElement) obj;
    if (doc == null) {
      if (other.doc != null) {
        return false;
      }
    } else if (!doc.equals(other.doc)) {
      return false;
    }
    if (name == null) {
      if (other.name != null) {
        return false;
      }
    } else if (!name.equals(other.name)) {
      return false;
    }
    return true;
  }

}
