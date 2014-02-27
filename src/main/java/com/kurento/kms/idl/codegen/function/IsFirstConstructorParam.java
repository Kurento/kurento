package com.kurento.kms.idl.codegen.function;

import java.util.List;

import com.kurento.kms.idl.model.Method;
import com.kurento.kms.idl.model.Param;
import com.kurento.kms.idl.model.RemoteClass;

import freemarker.ext.beans.StringModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class IsFirstConstructorParam implements TemplateMethodModelEx {

	@Override
	public Object exec(@SuppressWarnings("rawtypes") List arguments)
			throws TemplateModelException {

		RemoteClass thisRemoteClass = (RemoteClass) ((StringModel) arguments
				.get(0)).getWrappedObject();
		RemoteClass otherRemoteClass = (RemoteClass) ((StringModel) arguments
				.get(1)).getWrappedObject();

		if (!otherRemoteClass.getConstructors().isEmpty()) {

			Method method = otherRemoteClass.getConstructors().get(0);

			List<Param> params = method.getParams();

			if (params.isEmpty()) {
				return false;
			} else {
				return params.get(0).getType().getType() == thisRemoteClass;
			}

		} else {
			return false;
		}
	}

}
