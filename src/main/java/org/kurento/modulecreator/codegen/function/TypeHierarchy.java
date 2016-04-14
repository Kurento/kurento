package org.kurento.modulecreator.codegen.function;

import java.util.ArrayList;
import java.util.List;

import org.kurento.modulecreator.definition.RemoteClass;
import org.kurento.modulecreator.definition.Return;
import org.kurento.modulecreator.definition.TypeRef;

import freemarker.ext.beans.StringModel;
import freemarker.template.TemplateMethodModelEx;
import freemarker.template.TemplateModelException;

public class TypeHierarchy implements TemplateMethodModelEx {

  @Override
  public Object exec(@SuppressWarnings("rawtypes") List arguments) throws TemplateModelException {

    Object type = arguments.get(0);

    if (type instanceof StringModel) {
      type = ((StringModel) type).getWrappedObject();
      if (type instanceof Return) {
        type = ((Return) type).getType();
      }
    }

    if (type instanceof TypeRef) {
      type = ((TypeRef) type).getType();
    }

    if (type instanceof RemoteClass) {
      RemoteClass remoteClass = (RemoteClass) type;
      TypeRef parent = remoteClass.getExtends();
      ArrayList<String> list = new ArrayList<String>();

      while (parent != null) {
        String moduleName = parent.getModule().getName();

        if (moduleName == null) {
          moduleName = "";
        }

        if (moduleName.equals("core") || moduleName.equals("elements")
            || moduleName.equals("filters")) {
          moduleName = "kurento";
        }

        if (!moduleName.isEmpty()) {
          moduleName += ".";
        }

        list.add(moduleName + parent.getName());

        if (parent.getType() instanceof RemoteClass) {
          parent = ((RemoteClass) parent.getType()).getExtends();
        }
      }

      return list;
    } else {
      throw new TemplateModelException("Received invalid type, only RemoteClass is accepted");
    }

  }
}