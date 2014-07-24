package com.kurento.modulecreator.codegen.function;

import java.util.LinkedList;
import java.util.List;

import com.kurento.modulecreator.descriptor.ComplexType;
import com.kurento.modulecreator.descriptor.Method;
import com.kurento.modulecreator.descriptor.Param;
import com.kurento.modulecreator.descriptor.Property;
import com.kurento.modulecreator.descriptor.RemoteClass;
import com.kurento.modulecreator.descriptor.Return;
import com.kurento.modulecreator.descriptor.Type;

import freemarker.ext.beans.StringModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class RemoteClassDependencies implements TemplateMethodModelEx {

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

		List<Type> types = new LinkedList<Type>();

		if (type instanceof RemoteClass) {
			RemoteClass remoteClass = (RemoteClass) type;

			if (remoteClass.getConstructor() != null) {
				addMethodTypes(types, remoteClass.getConstructor());
			}

			for (Method method : remoteClass.getMethods()) {
				addMethodTypes(types, method);
			}

			for (Property property : remoteClass.getProperties()) {
				addDependency(types, property.getType().getType());
			}

			if (remoteClass.getExtends() != null)
				types.remove(remoteClass.getExtends().getType());

			types.remove(remoteClass);
		}

		types = removeDuplicates(types);

		return types;
	}

	private void addDependency(List<Type> dependencies, Type type) {
		if (type instanceof RemoteClass || type instanceof ComplexType) {
			dependencies.add(type);
		}
	}

	private List<Type> removeDuplicates(List<Type> original) {
		List<Type> types = new LinkedList<Type>();

		for (Type t : original) {
			if (!types.contains(t))
				types.add(t);
		}

		return types;
	}

	private void addMethodTypes(List<Type> dependencies, Method method) {
		for (Param p : method.getParams()) {
			addDependency(dependencies, p.getType().getType());
		}

		Return ret = method.getReturn();

		if (ret != null) {
			addDependency(dependencies, ret.getType().getType());
		}
	}
}
