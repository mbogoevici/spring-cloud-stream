/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.stream.binder;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.StringUtils;

/**
 * @author Marius Bogoevici
 */
public class DefaultBinderRegistry<T> implements BinderRegistry<T>, DisposableBean, ApplicationContextAware {

	private final Map<String, BinderConfiguration> binderConfigurations;

	private ConfigurableApplicationContext applicationContext;

	private Map<String, BinderInstanceHolder<T>> binderInstanceCache = new HashMap<>();

	public DefaultBinderRegistry(Map<String, BinderConfiguration> binderConfigurations) {
		this.binderConfigurations = new HashMap<>(binderConfigurations);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = (ConfigurableApplicationContext) applicationContext;
	}

	@Override
	public void destroy() throws Exception {
		for (Map.Entry<String, BinderInstanceHolder<T>> entry : binderInstanceCache.entrySet()) {
			BinderInstanceHolder<T> binderInstanceHolder = entry.getValue();
			binderInstanceHolder.getBinderContext().close();
		}
	}

	@Override
	public synchronized Binder<T> getBinder(String configurationName) {
		if (configurationName == null) {
			configurationName = "";
		}
		if (!binderInstanceCache.containsKey(configurationName)) {
			BinderConfiguration binderConfiguration;
			if (StringUtils.isEmpty(configurationName)) {
				if (binderConfigurations.size() == 0) {
					throw new IllegalStateException(
							"A default binder has been requested, but there there is no binder available");
				}
				else if (binderConfigurations.size() == 1) {
					binderConfiguration = binderConfigurations.values().iterator().next();
				}
				else {
					throw new IllegalStateException(
							"A default binder has been requested, but there is more than one binder available: "
									+ StringUtils.collectionToCommaDelimitedString(binderConfigurations.keySet()));
				}
			}
			else {
				binderConfiguration = binderConfigurations.get(configurationName);
			}
			if (binderConfiguration == null) {
				throw new IllegalStateException("Unknown binder configuration:" + configurationName);
			}
			Properties binderProperties = binderConfiguration.getProperties();
			Properties defaultProperties = binderProperties == null ? new Properties() : binderProperties;
			SpringApplicationBuilder springApplicationBuilder =
					new SpringApplicationBuilder(binderConfiguration.getBinderType().getConfigurationClasses())
							.bannerMode(Mode.OFF)
							.properties(defaultProperties)
							.web(false);
			springApplicationBuilder.parent(applicationContext);
			ConfigurableApplicationContext binderProducingContext =
					springApplicationBuilder.run("--spring.jmx.enabled=false");
			@SuppressWarnings("unchecked")
			Binder<T> binder = (Binder<T>) binderProducingContext.getBean(Binder.class);
			binderInstanceCache.put(configurationName, new BinderInstanceHolder<>(binder, binderProducingContext));
		}
		return binderInstanceCache.get(configurationName).getBinderInstance();
	}

	static class BinderInstanceHolder<T> {

		private Binder<T> binderInstance;

		private ConfigurableApplicationContext binderContext;

		public BinderInstanceHolder(Binder<T> binderInstance, ConfigurableApplicationContext binderContext) {
			this.binderInstance = binderInstance;
			this.binderContext = binderContext;
		}

		public Binder<T> getBinderInstance() {
			return binderInstance;
		}

		public ConfigurableApplicationContext getBinderContext() {
			return binderContext;
		}
	}
}
