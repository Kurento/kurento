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
package org.kurento.modulecreator.codegen.function;

import java.util.List;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class EscapeString implements TemplateMethodModelEx {

  @Override
  public Object exec(@SuppressWarnings("rawtypes") List arguments) throws TemplateModelException {

    if (arguments.get(0) == null) {
      return "";
    }

    return escapeString(arguments.get(0).toString());
  }

  private String escapeString(String str) {
    StringBuilder sb = new StringBuilder();

    if (str == null) {
      return null;
    }

    for (char ch : str.toCharArray()) {
      if (ch > 0xfff) {
        sb.append("\\u" + Integer.toHexString(ch).toUpperCase());
      } else if (ch > 0xff) {
        sb.append("\\u0" + Integer.toHexString(ch).toUpperCase());
      } else if (ch > 0x7f) {
        sb.append("\\u00" + Integer.toHexString(ch).toUpperCase());
      } else if (ch < 32) {
        switch (ch) {
          case '\b':
            sb.append('\\');
            sb.append('b');
            break;
          case '\n':
            sb.append('\\');
            sb.append('n');
            break;
          case '\t':
            sb.append('\\');
            sb.append('t');
            break;
          case '\f':
            sb.append('\\');
            sb.append('f');
            break;
          case '\r':
            sb.append('\\');
            sb.append('r');
            break;
          default:
            if (ch > 0xf) {
              sb.append("\\u00" + Integer.toHexString(ch).toUpperCase());
            } else {
              sb.append("\\u000" + Integer.toHexString(ch).toUpperCase());
            }
            break;
        }
      } else {
        switch (ch) {
          case '"':
            sb.append('\\');
            sb.append('"');
            break;
          case '\\':
            sb.append('\\');
            sb.append('\\');
            break;
          default:
            sb.append(ch);
            break;
        }
      }
    }
    return sb.toString();
  }
}
