package com.kurento.ktool.rom.processor.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Model {

	private static Logger LOG = LoggerFactory.getLogger(Model.class);

	public static final PrimitiveType STRING = new PrimitiveType("String");
	public static final PrimitiveType BOOLEAN = new PrimitiveType("boolean");
	public static final PrimitiveType INT = new PrimitiveType("int");
	public static final PrimitiveType FLOAT = new PrimitiveType("float");

	private String name;
	private List<RemoteClass> remoteClasses;
	private List<ComplexType> complexTypes;
	private List<Event> events;

	private transient Map<String, RemoteClass> remoteClassesMap;
	private transient Map<String, Event> eventsMap;
	private transient Map<String, ComplexType> complexTypesMap;

	private transient Map<String, Type> types;

	public Model() {
		this.remoteClasses = new ArrayList<RemoteClass>();
		this.complexTypes = new ArrayList<ComplexType>();
		this.events = new ArrayList<Event>();
	}

	public Model(List<RemoteClass> remoteClasses, List<ComplexType> types,
			List<Event> events) {
		super();
		this.remoteClasses = remoteClasses;
		this.complexTypes = types;
		this.events = events;

		populateModel();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		Model other = (Model) obj;
		if (events == null) {
			if (other.events != null) {
				return false;
			}
		} else if (!events.equals(other.events)) {
			return false;
		}
		if (remoteClasses == null) {
			if (other.remoteClasses != null) {
				return false;
			}
		} else if (!remoteClasses.equals(other.remoteClasses)) {
			return false;
		}
		if (complexTypes == null) {
			if (other.complexTypes != null) {
				return false;
			}
		} else if (!complexTypes.equals(other.complexTypes)) {
			return false;
		}
		return true;
	}

	public List<Event> getEvents() {
		return events;
	}

	public List<RemoteClass> getRemoteClasses() {
		return remoteClasses;
	}

	public List<ComplexType> getComplexTypes() {
		return complexTypes;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((events == null) ? 0 : events.hashCode());
		result = prime * result
				+ ((remoteClasses == null) ? 0 : remoteClasses.hashCode());
		result = prime * result
				+ ((complexTypes == null) ? 0 : complexTypes.hashCode());
		return result;
	}

	public void addEvent(Event event) {
		this.events.add(event);
	}

	public void addRemoteClass(RemoteClass remoteClass) {
		this.remoteClasses.add(remoteClass);
	}

	public void addType(ComplexType type) {
		this.complexTypes.add(type);
	}

	public void setEvents(List<Event> events) {
		this.events = events;
	}

	public void setRemoteClasses(List<RemoteClass> remoteClasses) {
		this.remoteClasses = remoteClasses;
	}

	public void setComplexTypes(List<ComplexType> types) {
		this.complexTypes = types;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String toString() {
		return "Model [remoteClasses=" + remoteClasses + ", types="
				+ complexTypes + ", events=" + events + "]";
	}

	public void populateModel(List<Model> dependencyModels) {

		remoteClassesMap = populateNamedElements(this.remoteClasses);
		eventsMap = populateNamedElements(this.events);
		complexTypesMap = populateNamedElements(this.complexTypes);

		types = new HashMap<String, Type>();
		types.putAll(remoteClassesMap);
		types.putAll(eventsMap);
		types.putAll(complexTypesMap);
		put(types, BOOLEAN);
		put(types, STRING);
		put(types, INT);
		put(types, FLOAT);

		Map<String, Type> allTypes = new HashMap<String, Type>(types);

		for (Model dependencyModel : dependencyModels) {
			allTypes.putAll(dependencyModel.getTypes());
		}

		resolveTypeRefs(remoteClasses, allTypes);
		resolveTypeRefs(events, allTypes);
		resolveTypeRefs(complexTypes, allTypes);
	}

	private Map<String, ? extends Type> getTypes() {
		return types;
	}

	public void populateModel() {
		populateModel(Collections.<Model> emptyList());
	}

	private void put(Map<String, ? super Type> types, Type t) {
		types.put(t.getName(), t);
	}

	private void resolveTypeRefs(List<? extends ModelElement> modelElements,
			Map<String, Type> baseTypes) {
		for (ModelElement modelElement : modelElements) {
			if (modelElement instanceof TypeRef) {
				TypeRef typeRef = (TypeRef) modelElement;
				Type baseType = baseTypes.get(typeRef.getName());
				if (baseType == null) {
					LOG.warn("The type '" + typeRef.getName()
							+ "' is not defined");
				} else {
					typeRef.setType(baseType);
				}

			} else {
				resolveTypeRefs(modelElement.getChildren(), baseTypes);
			}
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends NamedElement> Map<String, T> populateNamedElements(
			List<? extends T> elements) {
		Map<String, T> elementsMap = new HashMap<String, T>();
		for (NamedElement element : elements) {
			elementsMap.put(element.getName(), (T) element);
		}
		return elementsMap;
	}

	public RemoteClass getRemoteClass(String remoteClassName) {
		return remoteClassesMap.get(remoteClassName);
	}

	public ComplexType getType(String typeName) {
		return complexTypesMap.get(typeName);
	}

	public Event getEvent(String eventName) {
		return eventsMap.get(eventName);
	}

	public void expandMethodsWithOpsParams() {
		for (RemoteClass remoteClass : remoteClassesMap.values()) {
			remoteClass.expandMethodsWithOpsParams();
		}
	}

	public void addElements(Model model) {
		this.complexTypes.addAll(model.complexTypes);
		this.remoteClasses.addAll(model.remoteClasses);
		this.events.addAll(model.events);
	}

}
