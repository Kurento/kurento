package com.kurento.ktool.rom.processor.codegen.function;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.kurento.ktool.rom.processor.model.Return;
import com.kurento.ktool.rom.processor.model.TypeRef;

import freemarker.ext.beans.StringModel;
import freemarker.template.TemplateBooleanModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class CppObjectType implements TemplateMethodModelEx {

	private final Set<String> nativeTypes;

	public CppObjectType() {
		nativeTypes = new HashSet<String>();

		nativeTypes.add("float");
		nativeTypes.add("int");
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

		boolean isParam = true;
		String prefix = "";
		String suffix = "";

		if (arguments.size() > 1) {
			isParam = ((TemplateBooleanModel) arguments.get(1)).getAsBoolean();
		}

		if (arguments.size() > 2) {
			prefix = arguments.get(2).toString();
		}

		if (arguments.size() > 3) {
			suffix = arguments.get(3).toString();
		}

		if (type == null) {
			return "void";
		}

		if (type instanceof TypeRef) {
			TypeRef typeRef = (TypeRef) type;
			if (typeRef.isList()) {
				if (isParam)
					return "const std::vector<"
							+ getTypeAsString(typeRef.getName(), false, prefix,
									suffix) + ">&";
				else
					return "std::vector<"
							+ getTypeAsString(typeRef.getName(), false, prefix,
									suffix) + ">";
			} else {
				return getTypeAsString(typeRef.getName(), isParam, prefix,
						suffix);
			}
		}

		return getTypeAsString(type.toString(), isParam, prefix, suffix);
	}

	private String getTypeAsString(String typeName, boolean isParam,
			String prefix, String suffix) {
		if (typeName.equals("boolean")) {
			return "bool";
		} else if (typeName.equals("String")) {
			if (isParam) {
				return "const std::string&";
			} else {
				return "std::string";
			}
		} else if (nativeTypes.contains(typeName)) {
			return typeName;
		} else {
			return "std::shared_ptr<" + prefix + typeName + suffix + ">";
		}
	}
}
