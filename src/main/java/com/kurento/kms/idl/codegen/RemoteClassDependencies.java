package com.kurento.kms.idl.codegen;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.kurento.kms.idl.model.ComplexType;
import com.kurento.kms.idl.model.Method;
import com.kurento.kms.idl.model.Param;
import com.kurento.kms.idl.model.RemoteClass;
import com.kurento.kms.idl.model.Return;
import com.kurento.kms.idl.model.Type;

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

		Set<Type> types = new HashSet<Type>();

		if (type instanceof RemoteClass) {
			RemoteClass remoteClass = (RemoteClass) type;

			for (Method method : remoteClass.getConstructors()) {
				types.addAll(getMethodTypes(method));
			}

			for (Method method : remoteClass.getMethods()) {
				types.addAll(getMethodTypes(method));
			}

			if (remoteClass.getExtends() != null)
				types.remove(remoteClass.getExtends().getType());

			types.remove(remoteClass);
		}

		return types;
	}

	private Set<Type> getMethodTypes(Method method) {
		Set<Type> types = new HashSet<Type>();

		for (Iterator<Param> paramIt = method.getParams().iterator(); paramIt
				.hasNext();) {
			Param p = paramIt.next();

			if (p.getType().getType() instanceof RemoteClass
					|| p.getType().getType() instanceof ComplexType) {
				types.add(p.getType().getType());
			}

		}

		Return ret = method.getReturn();

		if (ret != null
				&& (ret.getType().getType() instanceof RemoteClass || ret
						.getType().getType() instanceof ComplexType))
			types.add(ret.getType().getType());

		return types;
	}
}
