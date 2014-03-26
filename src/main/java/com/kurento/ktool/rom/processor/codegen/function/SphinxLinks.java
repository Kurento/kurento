package com.kurento.ktool.rom.processor.codegen.function;

import java.util.List;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

/**
 * 
 * Replace sphinx/restructured text roles by javadoc equivalents in Kurento
 * model.json documentation
 * 
 * @author Santiago Gala (sgala@apache.org)
 * 
 */
public class SphinxLinks implements TemplateMethodModelEx {

	/**
	 * 
	 * Takes a string and replaces occurrences of rst/sphinx with kurento domain
	 * markup with javadoc equivalents.
	 * 
	 * @param arguments
	 *            A list of arguments from the call. It only processes the first
	 *            one as a String
	 * 
	 * @see freemarker.template.TemplateMethodModelEx#exec(java.util.List)
	 */
	@SuppressWarnings("rawtypes")
	@Override
	public Object exec(List arguments)
			throws TemplateModelException {

		// TODO: `<text>`, ** and *, :rom:meth/attr...
		String[][] toReplace = {
				{ ":term:`m(.*?)<(.*?)>`", // Kurento Glossary Term, alt
						"<a href=\"http://www.kurento.org/glossary.html#term-$2\">$1</a>" },
				{ ":term:`(.*?)`", // Kurento Glossary Term
						"<a href=\"http://www.kurento.org/glossary.html#term-$1\">$1</a>" },
				{ ":wikipedia:`(.*?),(.*?)`", // Kurento wikipedia, alt
						"<a href=\"http://$1.wikipedia.org/wiki/$2\">$2</a>" },
				{ ":wikipedia:`(.*?)<(.*?),(.*?)>`", // Kurento wikipedia
						"<a href=\"http://$2.wikipedia.org/wiki/$3\">$1</a>" },
				{ ":java:ref:`(.*?)<(.*?)>`", // java ref, alternate title
						"{@link $1 $2}" }, { ":java:ref:`(.*?)`", // java ref
						"{@link $1}" },
				{ ":rom:cls:`(.*?)<([^`]*?)>`", "{@link $1 $2}" },
				{ ":rom:cls:`(.*?)`", "{@link $1}" },
				{ ":rom:meth:`(.*?)<([^`]*?)>`", "{@link #$1 $2}" },
				{ ":rom:meth:`(.*?)`", "{@link #$1}" },
				{ ":rom:attr:`(.*?)<([^`]*?)>`", "{@link #$1 $2}" },
				{ ":rom:attr:`(.*?)`", "{@link #$1}" },
				{ ":rom:evt:`(.*?)<([^`]*?)>`", "{@link $1Event $2}" },
				{ ":rom:evt:`(.*?)`", "{@link $1Event}" },
				{ ":author:", "@author" }, // author
				{ ":since:", "@since" }, // since
				{ "``(.*?)``", "<code>$1</code>" },
				{ "\\.\\.\\s+todo::(.*?)", "<hr/><b>TODO</b>$1" },
				{ "\\.\\.\\s+note::(.*?)", "<hr/><b>Note</b>$1" },
		};

		String typeName = arguments.get(0).toString();
		String res = typeName;

		for (String[] each : toReplace) {
			res = res.replaceAll("(?ms)" + each[0], each[1]);
		}

		return res;
	}

}
