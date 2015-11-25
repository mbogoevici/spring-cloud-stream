/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.cloud.stream.binder.config;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.stream.binder.BinderConfiguration;
import org.springframework.cloud.stream.binder.BinderConfigurationRegistry;
import org.springframework.cloud.stream.binder.BinderRegistry;
import org.springframework.cloud.stream.binder.BinderType;
import org.springframework.cloud.stream.binder.BinderTypeRegistry;
import org.springframework.cloud.stream.binder.DefaultBinderConfigurationRegistry;
import org.springframework.cloud.stream.binder.DefaultBinderRegistry;
import org.springframework.cloud.stream.binder.DefaultBinderTypeRegistry;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.core.io.support.PropertiesLoaderUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * @author Marius Bogoevici
 */
@Configuration
public class BinderTypeRegistryAutoConfiguration {

	@Autowired
	ConfigurableApplicationContext configurableApplicationContext;

	@Bean
	@ConditionalOnMissingBean(BinderRegistry.class)
	public BinderRegistry<MessageChannel> binderRegistry(BinderTypeRegistry binderTypeRegistry,
	                                                     BinderConfigurationRegistry binderConfigurationRegistry) {
		return new DefaultBinderRegistry<>(binderConfigurationRegistry.getAll());
	}

	@Bean
	@ConditionalOnMissingBean(BinderConfigurationRegistry.class)
	public BinderConfigurationRegistry binderConfigurationRegistry(BinderTypeRegistry binderTypeRegistry) {
		Map<String,BinderConfiguration> binderConfigurations = new HashMap<>();
		for (Map.Entry<String, BinderType> entry : binderTypeRegistry.getAll().entrySet()) {
			binderConfigurations.put(entry.getKey(),
					new BinderConfiguration(entry.getValue(), new Properties()));
		}
		return new DefaultBinderConfigurationRegistry(binderConfigurations);
	}

	@Bean
	@ConditionalOnMissingBean(BinderTypeRegistry.class)
	public BinderTypeRegistry binderTypeRegistry() {
		Map<String, BinderType> binderTypes = new HashMap<>();
		ClassLoader classLoader = configurableApplicationContext.getClassLoader();
		if (classLoader == null) {
			classLoader = BinderTypeRegistryAutoConfiguration.class.getClassLoader();
		}
		try {
			Enumeration<URL> resources = classLoader.getResources("META-INF/spring.binders");
			if (resources == null || !resources.hasMoreElements()) {
				throw new BeanCreationException("Cannot create binder factory, no `META-INF/spring.binders` " +
						"resources found on the classpath");
			}
			while (resources.hasMoreElements()) {
				URL url = resources.nextElement();
				UrlResource resource = new UrlResource(url);
				for (BinderType binderType : parseBinderConfigurations(classLoader, resource)) {
					binderTypes.put(binderType.getDefaultName(), binderType);
				}
			}
		}
		catch (IOException | ClassNotFoundException e) {
			throw new BeanCreationException("Cannot create binder factory:", e);
		}
		return new DefaultBinderTypeRegistry(binderTypes);
	}


	static Collection<BinderType> parseBinderConfigurations(ClassLoader classLoader, Resource resource)
			throws IOException, ClassNotFoundException {
		Properties properties = PropertiesLoaderUtils.loadProperties(resource);
		Collection<BinderType> parsedBinderConfigurations = new ArrayList<>();
		for (Map.Entry<?, ?> entry : properties.entrySet()) {
			String binderType = (String) entry.getKey();
			String[] binderConfigurationClassNames =
					StringUtils.commaDelimitedListToStringArray((String)entry.getValue());
			Class[] binderConfigurationClasses = new Class[binderConfigurationClassNames.length];
			int i = 0;
			for (String binderConfigurationClassName : binderConfigurationClassNames) {
				binderConfigurationClasses[i++] = ClassUtils.forName(binderConfigurationClassName, classLoader);
			}
			parsedBinderConfigurations.add(new BinderType(binderType, binderConfigurationClasses));
		}
		return parsedBinderConfigurations;
	}

}
