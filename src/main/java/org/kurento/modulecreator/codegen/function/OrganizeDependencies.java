package org.kurento.modulecreator.codegen.function;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.kurento.modulecreator.definition.RemoteClass;
import org.kurento.modulecreator.definition.Type;

import freemarker.template.SimpleSequence;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class OrganizeDependencies implements TemplateMethodModelEx {

	@Override
	public Object exec(@SuppressWarnings("rawtypes") List arguments)
			throws TemplateModelException {

		Map<String, String> text = new HashMap<String, String>();

		if (arguments.size() != 2) {
			throw new TemplateModelException("2 argument is required");
		}

		if (!(arguments.get(0) instanceof SimpleSequence)) {
			throw new TemplateModelException("Class not expected");
		}
		SimpleSequence seq = (SimpleSequence) arguments.get(0);
		boolean isImpl = ((TemplateBooleanModel) arguments.get(1))
				.getAsBoolean();

		for (Object argument : seq.toList()) {
			if (argument instanceof Type) {
				Type dependency = (Type) argument;
				String namespace = dependency.getModule().getCode()
						.getImplementation().get("cppNamespace");
				String classesText = "";
				if (text.containsKey(namespace)) {
					// insert new class in existing namespace
					classesText = text.get(namespace);
					text.remove(namespace);
					classesText += "\n";
				} else {
					// create new namespace and insert new class
					for (String a : namespace.split("\\::")) {
						classesText += "namespace " + a + "\n{\n";
					}
				}

				boolean isClass = false;
				for (RemoteClass b : dependency.getModule().getRemoteClasses()) {
					if (b.getName().equals(dependency.getName())) {
						isClass = true;
					}
				}

				if (isImpl && isClass) {
					classesText += "class " + dependency.getName() + "Impl;";
				} else {
					classesText += "class " + dependency.getName() + ";";
				}
				text.put(namespace, classesText);
			}
		}

		Iterator<String> it = text.keySet().iterator();
		String ret = "";
		while (it.hasNext()) {
			ret += "\n";
			String key = it.next();
			ret += text.get(key);
			String[] split = key.split("\\::");
			ret += "\n";
			for (int i = split.length - 1; i >= 0; i--) {
				ret += "} /* " + split[i] + " */\n";
			}
		}

		return ret;
	}

}
