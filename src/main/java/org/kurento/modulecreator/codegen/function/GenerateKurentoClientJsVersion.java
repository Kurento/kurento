package org.kurento.modulecreator.codegen.function;

import java.util.List;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class GenerateKurentoClientJsVersion implements TemplateMethodModelEx {

	@Override
	public Object exec(@SuppressWarnings("rawtypes") List arguments) throws TemplateModelException {

		String version = arguments.get(0).toString();

		if (version.endsWith("-dev")) {
			return "Kurento/kurento-client-js";
		}

		return version;
	}

}
