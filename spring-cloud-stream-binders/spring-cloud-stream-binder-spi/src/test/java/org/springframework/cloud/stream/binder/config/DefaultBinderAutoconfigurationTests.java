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

import static org.hamcrest.Matchers.arrayContaining;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;

import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cloud.stream.binder.Binder;
import org.springframework.cloud.stream.binder.BinderConfigurationRegistry;
import org.springframework.cloud.stream.binder.BinderRegistry;
import org.springframework.cloud.stream.binder.BinderTypeRegistry;
import org.springframework.cloud.stream.binder.stub1.StubBinder1;
import org.springframework.cloud.stream.binder.stub1.StubBinder1Configuration;
import org.springframework.cloud.stream.binder.stub2.StubBinder2;
import org.springframework.cloud.stream.binder.stub2.StubBinder2ConfigurationA;
import org.springframework.cloud.stream.binder.stub2.StubBinder2ConfigurationB;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.util.ObjectUtils;

/**
 * @author Marius Bogoevici
 */
public class DefaultBinderAutoconfigurationTests {

	@Test
	public void loadBinderTypeRegistry() throws Exception {
		try {
			ConfigurableApplicationContext context = createBinderTestContext();
		}
		catch (BeanCreationException e) {
			assertThat(e.getMessage(),containsString("Cannot create binder factory, no `META-INF/spring.binders` " +
					"resources found on the classpath"));
		}
	}

	@Test
	public void loadBinderTypeRegistryWithOneBinder() throws Exception {
		ConfigurableApplicationContext context = createBinderTestContext("binder1");

		BinderTypeRegistry binderTypeRegistry = context.getBean(BinderTypeRegistry.class);
		assertThat(binderTypeRegistry, notNullValue());
		assertThat(binderTypeRegistry.getAll().size(), equalTo(1));
		assertThat(binderTypeRegistry.getAll(), hasKey("binder1"));
		assertThat(binderTypeRegistry.get("binder1"),
				hasProperty("configurationClasses", arrayContaining(StubBinder1Configuration.class)));

		BinderConfigurationRegistry binderConfigurationRegistry = context.getBean(BinderConfigurationRegistry.class);
		assertThat(binderConfigurationRegistry.getAll().size(), equalTo(1));
		assertThat(binderConfigurationRegistry.getAll(), hasKey("binder1"));
		assertThat(binderConfigurationRegistry.get("binder1"),
				hasProperty("binderType", equalTo(binderTypeRegistry.get("binder1"))));

		BinderRegistry binderRegistry = context.getBean(BinderRegistry.class);
		Binder binder1 = binderRegistry.getBinder("binder1");
		assertThat(binder1, instanceOf(StubBinder1.class));
	}

	@Test
	public void loadBinderTypeRegistryWithTwoBinders() throws Exception {

		ConfigurableApplicationContext context = createBinderTestContext("binder1", "binder2");
		BinderTypeRegistry binderTypeRegistry = context.getBean(BinderTypeRegistry.class);
		assertThat(binderTypeRegistry, notNullValue());
		assertThat(binderTypeRegistry.getAll().size(), equalTo(2));
		assertThat(binderTypeRegistry.getAll().keySet(), containsInAnyOrder("binder1", "binder2"));
		assertThat(binderTypeRegistry.get("binder1"),
				hasProperty("configurationClasses", arrayContaining(StubBinder1Configuration.class)));
		assertThat(binderTypeRegistry.get("binder2"),
				hasProperty("configurationClasses", arrayContaining(StubBinder2ConfigurationA.class,
						StubBinder2ConfigurationB.class)));

		BinderConfigurationRegistry binderConfigurationRegistry = context.getBean(BinderConfigurationRegistry.class);
		assertThat(binderConfigurationRegistry.getAll().size(), equalTo(2));
		assertThat(binderConfigurationRegistry.getAll().keySet(), containsInAnyOrder("binder1","binder2"));
		assertThat(binderConfigurationRegistry.get("binder1"),
				hasProperty("binderType", equalTo(binderTypeRegistry.get("binder1"))));
		assertThat(binderConfigurationRegistry.get("binder2"),
				hasProperty("binderType", equalTo(binderTypeRegistry.get("binder2"))));

		assertThat(binderConfigurationRegistry.getAll(), hasKey("binder1"));
		assertThat(binderConfigurationRegistry.get("binder1"),
				hasProperty("binderType", equalTo(binderTypeRegistry.get("binder1"))));

		BinderRegistry binderRegistry = context.getBean(BinderRegistry.class);
		Binder binder1 = binderRegistry.getBinder("binder1");
		assertThat(binder1, instanceOf(StubBinder1.class));
		Binder binder2 = binderRegistry.getBinder("binder2");
		assertThat(binder2, instanceOf(StubBinder2.class));
	}

	private static ConfigurableApplicationContext createBinderTestContext(String... additionalClasspathDirectories)
			throws IOException {
		URL[] urls = ObjectUtils.isEmpty(additionalClasspathDirectories) ?
				new URL[0] : new URL[additionalClasspathDirectories.length];
		if (!ObjectUtils.isEmpty(additionalClasspathDirectories)) {
			for (int i = 0; i < additionalClasspathDirectories.length; i++) {
				urls[i] = new URL(new ClassPathResource(additionalClasspathDirectories[i]).getURL().toString() + "/");
			}
		}
		ClassLoader classLoader = new URLClassLoader(urls, DefaultBinderAutoconfigurationTests.class.getClassLoader());
		return new SpringApplicationBuilder(SimpleApplication.class)
				.resourceLoader(new DefaultResourceLoader(classLoader))
				.web(false)
				.run();
	}

	@EnableAutoConfiguration
	public static class SimpleApplication {

	}
}
