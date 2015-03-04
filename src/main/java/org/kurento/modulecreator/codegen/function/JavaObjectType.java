package org.kurento.modulecreator.codegen.function;

import java.util.List;

import org.kurento.modulecreator.definition.Return;
import org.kurento.modulecreator.definition.Type;
import org.kurento.modulecreator.definition.TypeRef;

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
				return "java.util.List<"
						+ getTypeAsString(typeRef.getType(), true) + ">";
			} else if (typeRef.isMap()) {
				return "java.util.Map<String,"
						+ getTypeAsString(typeRef.getType(), true) + ">";
			} else {
				return getTypeAsString(typeRef.getType(), asObject);
			}
		}

		if (type instanceof Type) {
			return getTypeAsString((Type) type, asObject);
		}

		if (type instanceof String) {
			return getTypeAsString((String) type, asObject);
		}

		return type;
	}

	private String getTypeAsString(Type type, boolean asObject) {

		String typeName = type.getName();

		String typeAsString = getTypeAsString(typeName, asObject);

		if (typeAsString != null) {
			return typeAsString;
		}

		return type.getModule().getCode().getApi().get("java")
				.get("packageName")
				+ "." + typeName;
	}

	private String getTypeAsString(String typeName, boolean asObject) {

		if (asObject) {
			if (typeName.equals("int")) {
				return "Integer";
			} else if (typeName.equals("float")) {
				return "Float";
			} else if (typeName.equals("boolean")) {
				return "Boolean";
			}
		} else {
			if (typeName.equals("int") || typeName.equals("float")
					|| typeName.equals("boolean")) {
				return typeName;
			}
		}

		if (typeName.equals("String")) {
			return typeName;
		}

		return null;
	}

}