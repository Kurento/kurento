package org.kurento.modulecreator.codegen.function;

import java.util.List;

import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class PackageToFolder implements TemplateMethodModelEx {

	@Override
	public Object exec(@SuppressWarnings("rawtypes") List arguments) throws TemplateModelException {

		String packageName = arguments.get(0).toString();
		return packageName.replace(".", "/");
	}

}
