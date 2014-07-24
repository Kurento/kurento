package com.kurento.modulecreator.codegen.function;

import java.util.List;

import com.kurento.modulecreator.descriptor.Return;
import com.kurento.modulecreator.descriptor.TypeRef;

import freemarker.ext.beans.StringModel;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class JavaObjectType implements TemplateMethodModelEx {

	@Override
	public Object exec(@SuppressWarnings("rawtypes") List arguments)
			throws TemplateModelException {

		Object type = arguments.get(0);

		if (type instanceof StringModel) {
			type = ((StringModel) type).getWrappedObject();
			if (type instanceof Return) {
				type = ((Return) type).getType();
			}
		}

		boolean asObject = true;
		if (arguments.size() > 1) {
			asObject = ((TemplateBooleanModel) arguments.get(1)).getAsBoolean();
		}

		if (type == null) {
			if (asObject) {
				return "Void";
			} else {
				return "void";
			}
		}

		if (type instanceof TypeRef) {
			TypeRef typeRef = (TypeRef) type;
			if (typeRef.isList()) {
				return "List<" + getTypeAsString(typeRef.getName(), asObject)
						+ ">";
			} else {
				return getTypeAsString(typeRef.getName(), asObject);
			}
		}

		return getTypeAsString(type.toString(), asObject);
	}

	private String getTypeAsString(String typeName, boolean asObject) {
		if (asObject) {
			if (typeName.equals("int")) {
				return "Integer";
			} else if (typeName.equals("float")) {
				return "Float";
			} else if (typeName.equals("boolean")) {
				return "Boolean";
			} else {
				return typeName;
			}
		} else {
			return typeName;
		}
	}

}