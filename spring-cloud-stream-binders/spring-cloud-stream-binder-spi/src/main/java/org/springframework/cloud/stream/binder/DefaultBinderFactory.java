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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.Banner.Mode;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.binder.config.SeedConfiguration;
import org.springframework.cloud.stream.binder.config.TransportSpec;
import org.springframework.cloud.stream.binder.config.TransportSpec.TransportConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * @author Marius Bogoevici
 */
public class DefaultBinderFactory<T> implements BinderFactory<T>, DisposableBean,ApplicationContextAware {

	private final Map<String,BinderConfiguration> binderConfigurations;

	private ConfigurableApplicationContext applicationContext;

	@Autowired
	private TransportSpec transportSpec;

	private Map<String,BinderInstanceHolder<T>> binderInstanceCache = new HashMap<>();

	public DefaultBinderFactory(Map<String, BinderConfiguration> binderConfigurations) {
		this.binderConfigurations = binderConfigurations;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
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
			BinderConfiguration binderConfiguration = null;
			if (StringUtils.isEmpty(configurationName)) {
				if (binderConfigurations.size() == 1) {
					binderConfiguration = binderConfigurations.values().iterator().next();
				}
				else {
					throw new IllegalStateException("A default binder has been requested, but there is more than one " +
							"binder available");
				}
			}
			else {
				binderConfiguration = binderConfigurations.get(configurationName);
			}
			Properties binderProperties = null;
			if (binderConfiguration == null) {
				TransportConfiguration transportConfiguration = transportSpec.getTransports().get(configurationName);
				if (transportConfiguration == null) {
					throw new IllegalStateException("Unknown binder configuration:" + configurationName);
				} else {
					binderConfiguration = binderConfigurations.get(transportConfiguration.getType());
					binderProperties = transportConfiguration.getProperties();
					if (binderConfiguration == null) {
						throw new IllegalStateException("Unknown binder type:" + transportConfiguration.getType());
					}
				}
			}
			SpringApplicationBuilder springApplicationBuilder = new SpringApplicationBuilder(ObjectUtils.addObjectToArray(binderConfiguration.getConfiguration(),
					SeedConfiguration.class))
					.bannerMode(Mode.OFF)
					.properties(binderProperties == null ? new Properties() : binderProperties)
					.web(false);
			if (binderConfigurations.containsKey(configurationName) || "".equals(configurationName)) {
				springApplicationBuilder.parent(applicationContext);
			}
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
