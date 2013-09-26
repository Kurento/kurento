/*
 * (C) Copyright 2013 Kurento (http://kurento.org/)
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 */
package com.kurento.kmf.spring;

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
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.parentContext = applicationContext.getParent();
	}

	public ApplicationContext getParentContext() {
		return parentContext;
	}
}
