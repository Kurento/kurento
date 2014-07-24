package com.kurento.modulecreator.codegen.function;

import java.util.List;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class CamelToUnderscore implements TemplateMethodModelEx {

	@Override
	public Object exec(@SuppressWarnings("rawtypes") List arguments)
			throws TemplateModelException {

		Object typeName = arguments.get(0);

		String regex = "([a-z])([A-Z])";
		String replacement = "$1_$2";
		// return ((String) typeName).replaceAll(regex, replacement);
		return typeName.toString().replaceAll(regex, replacement).toUpperCase();
	}

}
