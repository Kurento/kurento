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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.kurento.modulecreator.definition.ComplexType;
import org.kurento.modulecreator.definition.Event;
import org.kurento.modulecreator.definition.Import;
import org.kurento.modulecreator.definition.ModuleDefinition;
import org.kurento.modulecreator.definition.RemoteClass;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

/**
 * Replace sphinx/restructured text roles by javadoc and jsdoc equivalents in Kurento kmd.json
 * documentation
 *
 * @author Santiago Gala (sgala@apache.org)
 * @author Jesús Leganés-Combarro 'piranna' (piranna@gmail.com)
 *
 */
public class SphinxLinks implements TemplateMethodModelEx {

  private static Pattern glossary_term_1 = Pattern.compile(":term:`([^`<]*?)`");
  private static Pattern glossary_term_2 = Pattern.compile(":term:`([^`<]*?)<([^`]*?)>`");

  private static String glossary_href = "<a href=\"http://www.kurento.org/docs/current/glossary.html#term-%s\">%s</a>";
  // TODO: `<text>`, ** and *, other markup...

  private final List<String[]> toReplace = new ArrayList<String[]>(Arrays.asList(new String[][] {
      // java ref
      { ":java:ref:`([^`]*?)<(.*?)>`", "{@link $2 $1}" }, { ":java:ref:`(.*?)`", "{@link $1}" },

      // Kurento ROM
      { ":rom:enum:`([^`]*?)`", "{@link $1}" },

      { ":rom:evt:`([^`]*?)<([^`]*?)>`", "{@link $2 $1Event}" },
      { ":rom:evt:`([^`]*?)`", "{@link $1Event}" },

      // JsDoc tags
      { ":author:", "@author" }, { ":since:", "@since" }, { "``([^`]*?)``", "<code>$1</code>" },
      { "\\.\\.\\s+todo::(.*?)", "<hr/><b>TODO</b>$1" },
      { "\\.\\.\\s+note::(.*?)", "<hr/><b>Note</b>$1" },

      // wikipedia
      { ":wikipedia:`(.*?),(.*?)`", "<a href=\"http://$1.wikipedia.org/wiki/$2\">$2</a>" },
      { ":wikipedia:`(.*?)<(.*?),(.*?)>`",
          "<a href=\"http://$2.wikipedia.org/wiki/$3\">$1</a>" } }));

  public SphinxLinks(ModuleDefinition module) {
    super();

    addModule(module);

    for (Import i : module.getImports()) {
      addModule(i.getModule());
    }
  }

  private void addModule(ModuleDefinition module) {
    addRemoteClasses(module);
    addComplexTypes(module);
    addEvents(module);
  }

  private void addRemoteClasses(ModuleDefinition module) {
    for (RemoteClass remoteClass : module.getRemoteClasses()) {
      String className = remoteClass.getName();
      String namePath = "module:" + module.getName();
      namePath += remoteClass.isAbstract() ? "/abstracts" : "";
      namePath += "." + className;

      toReplace.addAll(Arrays.asList(new String[][] {
          { ":rom:cls:`" + className + "`", "{@link " + namePath + " " + className + "}" },
          { ":rom:cls:`([^`]*?)<" + className + ">`", "{@link " + namePath + " $1}" } }));
    }
  }

  private void addComplexTypes(ModuleDefinition module) {
    for (ComplexType complexType : module.getComplexTypes()) {
      String typeName = complexType.getName();
      String namePath = "module:" + module.getName() + "/complexTypes." + typeName;

      toReplace.addAll(Arrays.asList(new String[][] {
          { ":rom:ref:`" + typeName + "`", "{@link " + namePath + " " + typeName + "}" },
          { ":rom:ref:`([^`]*?)<" + typeName + ">`", "{@link " + namePath + " $1}" } }));
    }
  }

  private void addEvents(ModuleDefinition module) {
    for (Event event : module.getEvents()) {
      String eventName = event.getName();
      String namePath = "module:" + module.getName() + "#event:" + eventName;

      toReplace.addAll(Arrays.asList(new String[][] {
          { ":rom:evt:`" + eventName + "`", "{@link " + namePath + " " + eventName + "}" },
          { ":rom:evt:`([^`]*?)<" + eventName + ">`", "{@link " + namePath + " $1}" } }));
    }
  }

  /**
   * Takes a string and replaces occurrences of rst/sphinx with kurento domain markup with javadoc
   * and jsdoc equivalents.
   *
   * @param arguments
   *          A list of arguments as Strings from the call. The first one is the rst/sphinx text,
   *          and the second optional one defines the current class full name.
   *
   * @see freemarker.template.TemplateMethodModelEx#exec(java.util.List)
   */
  @SuppressWarnings("rawtypes")
  @Override
  public Object exec(List arguments) throws TemplateModelException {

    // Classes
    String typeName = arguments.get(0).toString();
    String res = translate(typeName, toReplace);

    // Instance properties
    String classNamePath = arguments.size() > 1 ? "module:" + arguments.get(1).toString() : "";

    String instanceProperty = "{@link " + classNamePath + "#$1}";
    String instancePropertyAlt = "{@link " + classNamePath + "#$2 $1}";

    res = translate(res,
        Arrays.asList(new String[][] { { ":rom:meth:`([^`]*?)<([^`]*?)>`", instancePropertyAlt },
            { ":rom:meth:`([^`]*?)`", instanceProperty },
            { ":rom:attr:`([^`]*?)<([^`]*?)>`", instancePropertyAlt },
            { ":rom:attr:`([^`]*?)`", instanceProperty }, }));

    // Glosaries
    Matcher m2 = glossary_term_2.matcher(res);
    while (m2.find()) {
      res = res.substring(0, m2.start() - 1)
          + String.format(glossary_href, make_id(m2.group(2)), m2.group(1))
          + res.substring(m2.end() + 1);
    }

    m2 = glossary_term_1.matcher(res);
    while (m2.find()) {
      res = res.substring(0, m2.start())
          + String.format(glossary_href, make_id(m2.group(1)), m2.group(1))
          + res.substring(m2.end());
    }

    return res;
  }

  /**
   * Clone the python unicode translate method in legacy languages younger than python. python
   * docutils is public domain.
   *
   * @param text
   *          string for which translation is needed
   * @param patterns
   *          Array of arrays {target, replacement). The target is substituted by the replacement.
   * @return The translated string
   * @see http://docs.python.org/3/library/stdtypes.html#str.translate
   */
  public String translate(String text, List<String[]> patterns) {
    String res = text;

    for (String[] each : patterns) {
      res = res.replaceAll("(?ms)" + each[0], each[1]);
    }

    return res;
  }

  /**
   * Our use case is
   * {@code $ python -c "from docutils import nodes; print('term-'+nodes.make_id('QR'))" } , which
   * returns {@code term-qr } i.e., identifiers conforming to the regular expression
   * [a-z](-?[a-z0-9]+)*
   *
   * <p>
   * But there is a requirement to use <em>pure</em> java for this task. So we clone the function
   * here. python docutils is public domain.
   * </p>
   *
   * @see http ://code.nabla.net/doc/docutils/api/docutils/nodes/docutils.nodes. make_id.html
   */
  public String make_id(String txt) {
    // id = string.lower()
    String id = txt.toLowerCase();
    // if not isinstance(id, unicode):
    // id = id.decode()
    // id = id.translate(_non_id_translate_digraphs)
    id = translate(id, nonIdTranslateDigraphs);
    // id = id.translate(_non_id_translate)
    id = translate(id, nonIdTranslate);
    // # get rid of non-ascii characters.
    // # 'ascii' lowercase to prevent problems with turkish locale.
    // id = unicodedata.normalize('NFKD', id).\
    // encode('ascii', 'ignore').decode('ascii')
    // # shrink runs of whitespace and replace by hyphen
    // id = _non_id_chars.sub('-', ' '.join(id.split()))
    id = id.replaceAll("\\s+", " ").replaceAll(nonIdChars, "-");
    // id = _non_id_at_ends.sub('', id)
    id = id.replaceAll(nonIdAtEnds, "");
    // return str(id)
    return id;
  }

  // _non_id_chars = re.compile('[^a-z0-9]+')
  private final String nonIdChars = "[^a-z0-9]+";
  // _non_id_at_ends = re.compile('^[-0-9]+|-+$')
  private final String nonIdAtEnds = "^[-0-9]+|-+$";

  private final List<String[]> nonIdTranslate = Arrays.asList(new String[][] { { "\u00f8", "o" },
      // o with stroke
      { "\u0111", "d" }, // d with stroke
      { "\u0127", "h" }, // h with stroke
      { "\u0131", "i" }, // dotless i
      { "\u0142", "l" }, // l with stroke
      { "\u0167", "t" }, // t with stroke
      { "\u0180", "b" }, // b with stroke
      { "\u0183", "b" }, // b with topbar
      { "\u0188", "c" }, // c with hook
      { "\u018c", "d" }, // d with topbar
      { "\u0192", "f" }, // f with hook
      { "\u0199", "k" }, // k with hook
      { "\u019a", "l" }, // l with bar
      { "\u019e", "n" }, // n with long right leg
      { "\u01a5", "p" }, // p with hook
      { "\u01ab", "t" }, // t with palatal hook
      { "\u01ad", "t" }, // t with hook
      { "\u01b4", "y" }, // y with hook
      { "\u01b6", "z" }, // z with stroke
      { "\u01e5", "g" }, // g with stroke
      { "\u0225", "z" }, // z with hook
      { "\u0234", "l" }, // l with curl
      { "\u0235", "n" }, // n with curl
      { "\u0236", "t" }, // t with curl
      { "\u0237", "j" }, // dotless j
      { "\u023c", "c" }, // c with stroke
      { "\u023f", "s" }, // s with swash tail
      { "\u0240", "z" }, // z with swash tail
      { "\u0247", "e" }, // e with stroke
      { "\u0249", "j" }, // j with stroke
      { "\u024b", "q" }, // q with hook tail
      { "\u024d", "r" }, // r with stroke
      { "\u024f", "y" } // y with stroke
  });

  private final List<String[]> nonIdTranslateDigraphs = Arrays
      .asList(new String[][] { { "\u00df", "sz" }, // ligature
          // sz
          { "\u00e6", "ae" }, // ae
          { "\u0153", "oe" }, // ligature oe
          { "\u0238", "db" }, // db digraph
          { "\u0239", "qp" } // qp digraph
  });

}
