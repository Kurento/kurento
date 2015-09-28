package org.kurento.modulecreator.codegen.function;

import java.util.List;

import org.kurento.modulecreator.definition.Return;
import org.kurento.modulecreator.definition.TypeRef;

import freemarker.ext.beans.StringModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class InitializePropertiesValues implements TemplateMethodModelEx {

	@Override
	public Object exec(@SuppressWarnings("rawtypes") List arguments) throws TemplateModelException {

		if (arguments.size() != 1) {
			throw new TemplateModelException("This function expects 1 parameter");
		}
		Object type = arguments.get(0);
		String ret = "";

		if (type instanceof StringModel) {
			type = ((StringModel) type).getWrappedObject();
			if (type instanceof Return) {
				type = ((Return) type).getType();
			}
		}

		if (type instanceof TypeRef) {
			TypeRef typeRef = (TypeRef) type;
			if (typeRef.getName().equals("boolean")) {
				ret = "= false";
			} else if (typeRef.getName().equals("float") || typeRef.getName().equals("int")) {
				ret = "= 0";
			}
		} else {
			throw new TemplateModelException("Class not expected " + type);
		}
		return ret;
	}

}
