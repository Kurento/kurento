package com.kurento.kmf.spring;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * This class has the objective of finding out whether a root
 * WebApplicationContext has been created by the application developer. In that
 * case, that root application context must be the parent of the Kurento
 * application context. This parent is later used in Kurento application context
 * to override internal bean configurations with the potential customized
 * configurations that may have been defined by the application developer.
 * 
 * @author Luis López
 */
public class RootWebApplicationContextParentRecoverer implements
		ApplicationContextAware {

	private ApplicationContext parentContext;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.parentContext = applicationContext.getParent();
	}

	public ApplicationContext getParentContext() {
		return parentContext;
	}
}
