package com.kurento.ktool.rom.processor.model;

import java.util.ArrayList;
import java.util.List;

import com.google.gson.annotations.SerializedName;

public class RemoteClass extends Type {

	@SerializedName("extends")
	private TypeRef extendsProp;
	private List<Method> constructors;
	private List<Method> methods;
	private List<Property> properties;
	private List<TypeRef> events;
	private boolean abstractClass;

	public RemoteClass(String name, String doc, TypeRef extendsProp) {
		super(name, doc);
		this.extendsProp = extendsProp;
		this.constructors = new ArrayList<Method>();
		this.methods = new ArrayList<Method>();
		this.properties = new ArrayList<Property>();
		this.events = new ArrayList<TypeRef>();
	}

	public RemoteClass(String name, String doc, TypeRef extendsProp,
			List<Method> constructors, List<Method> methods,
			List<Property> properties, List<TypeRef> events) {
		super(name, doc);
		this.extendsProp = extendsProp;
		this.constructors = constructors;
		this.methods = methods;
		this.properties = properties;
		this.events = events;
	}

	public List<Method> getConstructors() {
		return constructors;
	}

	public List<TypeRef> getEvents() {
		return events;
	}

	public TypeRef getExtends() {
		return extendsProp;
	}

	public List<Method> getMethods() {
		return methods;
	}

	public boolean isAbstract() {
		return abstractClass;
	}

	public List<Property> getProperties() {
		return properties;
	}

	public void addConstructor(Method constructor) {
		this.constructors.add(constructor);
	}

	public void addMethod(Method method) {
		this.methods.add(method);
	}

	public void addProperty(Property property) {
		this.properties.add(property);
	}

	public void setAbstract(boolean abstractModel) {
		this.abstractClass = abstractModel;
	}

	public void setConstructors(List<Method> constructors) {
		this.constructors = constructors;
	}

	public void setProperties(List<Property> properties) {
		this.properties = properties;
	}

	public void setEvents(List<TypeRef> events) {
		this.events = events;
	}

	public void setExtendsProp(TypeRef extendsProp) {
		this.extendsProp = extendsProp;
	}

	public void setMethods(List<Method> methods) {
		this.methods = methods;
	}

	public boolean isAssignableTo(String remoteClassName) {
		if (this.getName().equals(remoteClassName)) {
			return true;
		} else {
			if (getExtends() != null) {
				return ((RemoteClass) getExtends().getType())
						.isAssignableTo(remoteClassName);
			} else {
				return false;
			}
		}
	}

	@Override
	public List<ModelElement> getChildren() {
		List<ModelElement> children = new ArrayList<ModelElement>();
		if (extendsProp != null) {
			children.add(extendsProp);
		}
		children.addAll(constructors);
		children.addAll(methods);
		children.addAll(events);
		return children;
	}

	@Override
	public String toString() {
		return "RemoteClass [extends=" + extendsProp + ", constructors="
				+ constructors + ", methods=" + methods + ", doc=" + getDoc()
				+ ", name=" + getName() + "]";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result + (abstractClass ? 1231 : 1237);
		result = prime * result
				+ ((constructors == null) ? 0 : constructors.hashCode());
		result = prime * result + ((events == null) ? 0 : events.hashCode());
		result = prime * result
				+ ((extendsProp == null) ? 0 : extendsProp.hashCode());
		result = prime * result + ((methods == null) ? 0 : methods.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!super.equals(obj)) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		RemoteClass other = (RemoteClass) obj;
		if (abstractClass != other.abstractClass) {
			return false;
		}
		if (constructors == null) {
			if (other.constructors != null) {
				return false;
			}
		} else if (!constructors.equals(other.constructors)) {
			return false;
		}
		if (events == null) {
			if (other.events != null) {
				return false;
			}
		} else if (!events.equals(other.events)) {
			return false;
		}
		if (extendsProp == null) {
			if (other.extendsProp != null) {
				return false;
			}
		} else if (!extendsProp.equals(other.extendsProp)) {
			return false;
		}
		if (methods == null) {
			if (other.methods != null) {
				return false;
			}
		} else if (!methods.equals(other.methods)) {
			return false;
		}
		return true;
	}

	public void expandMethodsWithOpsParams() {
		List<Method> newMethods = new ArrayList<Method>();
		for (Method method : this.methods) {
			newMethods.addAll(method.expandIfOpsParams());
		}
		this.methods.addAll(newMethods);
	}

}
