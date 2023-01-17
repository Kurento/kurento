/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
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

package org.kurento.jsonrpc;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Props implements Iterable<Prop> {

  private final Map<String, Object> props;

  public Props() {
    props = new HashMap<>();
  }

  public Props(Map<String, Object> props) {
    this.props = props;
  }

  public Props(String name, Object value) {
    this();
    add(name, value);
  }

  public Object getProp(String name) {
    return props.get(name);
  }

  public boolean hasProp(String name) {
    return props.keySet().contains(name);
  }

  public Props add(String property, Object value) {
    props.put(property, value);
    return this;
  }

  public Map<String, Object> getMap() {
    return props;
  }

  @Override
  public Iterator<Prop> iterator() {

    final Iterator<Map.Entry<String, Object>> entries = props.entrySet().iterator();

    Iterator<Prop> propsIterator = new Iterator<Prop>() {

      @Override
      public boolean hasNext() {
        return entries.hasNext();
      }

      @Override
      public Prop next() {
        Map.Entry<String, Object> entry = entries.next();
        return new PropImpl(entry.getKey(), entry.getValue());
      }

      @Override
      public void remove() {
        entries.remove();
      }
    };

    return propsIterator;
  }

  @Override
  public String toString() {
    return props.toString();
  }
  
  public Object removeProp(String name) {
		return props.remove(name);
	}

}
