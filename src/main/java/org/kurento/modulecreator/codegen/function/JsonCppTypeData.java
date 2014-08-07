package org.kurento.modulecreator.codegen.function;

import java.util.List;

import org.kurento.modulecreator.definition.ComplexType;
import org.kurento.modulecreator.definition.RemoteClass;
import org.kurento.modulecreator.definition.Return;
import org.kurento.modulecreator.definition.TypeRef;
import org.kurento.modulecreator.definition.ComplexType.TypeFormat;

import freemarker.ext.beans.StringModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class JsonCppTypeData implements TemplateMethodModelEx {

	public class JsonTypeData {
		String jsonMethod;
		String jsonValueType;
		String typeDescription;

		public String getJsonMethod() {
			return jsonMethod;
		}

		public String getJsonValueType() {
			return jsonValueType;
		}

		public String getTypeDescription() {
			return typeDescription;
		}
	}

	public JsonCppTypeData() {
	}

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

		if (type == null) {
			throw new TemplateModelException("Received invalid type");
		}

		if (type instanceof TypeRef) {
			TypeRef typeRef = (TypeRef) type;
			if (typeRef.isList()) {
				JsonTypeData data = new JsonTypeData();
				data.jsonMethod = "List";
				data.jsonValueType = "arrayValue";
				data.typeDescription = "list";
				return data;
			} else if (typeRef.getName().equals("String")) {
				JsonTypeData data = new JsonTypeData();
				data.jsonMethod = "String";
				data.jsonValueType = "stringValue";
				data.typeDescription = "string";
				return data;
			} else if (typeRef.getName().equals("int")) {
				JsonTypeData data = new JsonTypeData();
				data.jsonMethod = "Int";
				data.jsonValueType = "intValue";
				data.typeDescription = "integer";
				return data;
			} else if (typeRef.getName().equals("boolean")) {
				JsonTypeData data = new JsonTypeData();
				data.jsonMethod = "Bool";
				data.jsonValueType = "booleanValue";
				data.typeDescription = "boolean";
				return data;
			} else if (typeRef.getName().equals("double")
					|| typeRef.getName().equals("float")) {
				JsonTypeData data = new JsonTypeData();
				data.jsonMethod = "Double";
				data.jsonValueType = "realValue";
				data.typeDescription = "double";
				return data;
			} else if (typeRef.getType() instanceof ComplexType) {
				ComplexType complexType = (ComplexType) typeRef.getType();

				if (complexType.getTypeFormat() == TypeFormat.ENUM) {
					JsonTypeData data = new JsonTypeData();
					data.jsonMethod = "String";
					data.jsonValueType = "stringValue";
					data.typeDescription = "string";
					return data;
				} else if (complexType.getTypeFormat() == TypeFormat.REGISTER) {
					JsonTypeData data = new JsonTypeData();
					data.jsonMethod = "Object";
					data.jsonValueType = "objectValue";
					data.typeDescription = "object";
					return data;
				}
			} else if (typeRef.getType() instanceof RemoteClass) {
				JsonTypeData data = new JsonTypeData();
				data.jsonMethod = "String";
				data.jsonValueType = "stringValue";
				data.typeDescription = "string";
				return data;
			}

			throw new TemplateModelException("Unexpected type: " + type);
		}

		throw new TemplateModelException("Received invalid type");
	}
}
