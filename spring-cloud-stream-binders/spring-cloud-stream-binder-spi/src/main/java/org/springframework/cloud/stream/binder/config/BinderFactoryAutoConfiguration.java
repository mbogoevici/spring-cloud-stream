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

package org.springframework.cloud.stream.binder.config;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.stream.binder.BinderConfiguration;
import org.springframework.cloud.stream.binder.BinderFactory;
import org.springframework.cloud.stream.binder.DefaultBinderFactory;
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
@AutoConfigureOrder(Ordered.LOWEST_PRECEDENCE)
@EnableConfigurationProperties(TransportSpec.class)
public class BinderFactoryAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean(BinderFactory.class)
	public BinderFactory<MessageChannel> binderFactory() {

		Map<String,BinderConfiguration> binderConfigurationMap = new HashMap<>();
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		if (classLoader == null) {
			classLoader = BinderFactoryAutoConfiguration.class.getClassLoader();
		}

		try {
			Enumeration<URL> springBindersUrls = classLoader.getResources("META-INF/spring.binders");
			if (springBindersUrls == null) {
				throw new BeanCreationException("Cannot create binder factory, `META-INF/spring.binders` resources found on the classpath");
			}
			while (springBindersUrls.hasMoreElements()) {
				URL springBindersUrl = springBindersUrls.nextElement();
				UrlResource resource = new UrlResource(springBindersUrl);
				List<BinderConfiguration> discoveredBinderConfigurations = parseBinderConfigurations(classLoader, resource);
				for (BinderConfiguration discoveredBinderConfiguration : discoveredBinderConfigurations) {
					binderConfigurationMap.put(discoveredBinderConfiguration.getName(), discoveredBinderConfiguration);
				}
			}
		} catch (IOException | ClassNotFoundException e) {
			throw new BeanCreationException("Cannot create binder factory:", e);
		}
		return new DefaultBinderFactory<>(binderConfigurationMap);
	}

	static List<BinderConfiguration> parseBinderConfigurations(ClassLoader classLoader, Resource resource) throws IOException, ClassNotFoundException {
		Properties springBindersContent = PropertiesLoaderUtils.loadProperties(resource);
		List<BinderConfiguration> discoveredBinderConfigurations = new ArrayList<>();
		for (Map.Entry<Object, Object> binderConfiguration : springBindersContent.entrySet()) {
			String binderConfigurationName = binderConfiguration.getKey().toString();
			String[] binderConfigurationClassNames = StringUtils.commaDelimitedListToStringArray(binderConfiguration.getValue().toString());
			Class[] binderConfigurationClasses = new Class[binderConfigurationClassNames.length];
			int i = 0;
			for (String binderConfigurationClassName : binderConfigurationClassNames) {
				binderConfigurationClasses[i++] = ClassUtils.forName(binderConfigurationClassName, classLoader);
			}
			discoveredBinderConfigurations.add(new BinderConfiguration(binderConfigurationName, binderConfigurationClasses));
		}
		return discoveredBinderConfigurations;
	}
}
