/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.kurento.kmf.jsonrpcconnector.internal.server;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;

/**
 * Instantiates a target handler through a Spring {@link BeanFactory} and also
 * provides an equivalent destroy method. Mainly for internal use to assist with
 * initializing and destroying handlers with per-connection lifecycle.
 * 
 * @author Rossen Stoyanchev
 * @param <T>
 * @since 4.0
 */
class BeanCreatingHelper<T> implements BeanFactoryAware {

	private static final Logger logger = LoggerFactory
			.getLogger(BeanCreatingHelper.class);

	private AutowireCapableBeanFactory beanFactory;

	private final Class<? extends T> beanType;
	private final String beanName;
	private Class<?> createdBeanType;

	public BeanCreatingHelper(Class<? extends T> handlerType, String beanName) {
		this.beanType = handlerType;
		this.beanName = beanName;

		this.createdBeanType = beanType;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (beanFactory instanceof AutowireCapableBeanFactory) {
			this.beanFactory = (AutowireCapableBeanFactory) beanFactory;
		}
	}

	public Class<?> getCreatedBeanType() {
		return this.createdBeanType;
	}

	public void setCreatedBeanType(Class<?> createdBeanType) {
		this.createdBeanType = createdBeanType;
	}

	@SuppressWarnings("unchecked")
	public T createBean() {
		if (logger.isTraceEnabled()) {
			logger.trace("Creating instance for handler type {}", this.beanType);
		}
		if (this.beanFactory == null) {
			logger.warn("No BeanFactory available, attempting to use default constructor");
			return BeanUtils.instantiate(this.beanType);
		} else {

			if (beanType != null) {
				return this.beanFactory.createBean(this.beanType);
			} else {
				T bean = (T) beanFactory.getBean(beanName);
				createdBeanType = bean.getClass();
				return bean;
			}
		}
	}

	public void destroy(T handler) {
		if (this.beanFactory != null) {
			if (logger.isTraceEnabled()) {
				logger.trace("Destroying handler instance {}", handler);
			}
			this.beanFactory.destroyBean(handler);
		}
	}

	@Override
	public String toString() {
		return "BeanCreatingHelper [beanType=" + beanType + ", beanName="
				+ beanName + "]";
	}
}
