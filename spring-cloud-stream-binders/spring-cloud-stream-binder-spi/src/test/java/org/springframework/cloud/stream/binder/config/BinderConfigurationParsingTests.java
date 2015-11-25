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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.collection.IsArrayContaining.hasItemInArray;
import static org.hamcrest.collection.IsArrayContainingInAnyOrder.arrayContainingInAnyOrder;
import static org.hamcrest.core.CombinableMatcher.both;

import java.io.ByteArrayInputStream;
import java.util.Collection;

import org.junit.Assert;
import org.junit.Test;

import org.springframework.cloud.stream.binder.BinderType;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;

/**
 * @author Marius Bogoevici
 */
@SuppressWarnings("Duplicates")
public class BinderConfigurationParsingTests {

	private static ClassLoader classLoader = BinderConfigurationParsingTests.class.getClassLoader();

	@Test
	public void testParseOneBinderConfiguration() throws Exception {

		// this is just checking that resources are passed and classes are loaded properly
		// class values used here are not binder configurations
		String oneBinderConfiguration = "redis=java.lang.String";
		Resource resource = new InputStreamResource(new ByteArrayInputStream(oneBinderConfiguration.getBytes()));

		Collection<BinderType> binderConfigurations
				= BinderTypeRegistryAutoConfiguration.parseBinderConfigurations(classLoader, resource);

		Assert.assertNotNull(binderConfigurations);
		Assert.assertThat(binderConfigurations.size(), equalTo(1));
		Assert.assertThat(binderConfigurations, contains(
				both(hasProperty("defaultName", equalTo("redis"))).and(
						hasProperty("configurationClasses", hasItemInArray(String.class)))
		));
	}

	@Test
	public void testParseTwoBindersConfigurations() throws Exception {
		// this is just checking that resources are passed and classes are loaded properly
		// class values used here are not binder configurations
		String binderConfiguration = "redis=java.lang.String\n" +
				"rabbit=java.lang.Integer";
		Resource twoBinderConfigurationResource =
				new InputStreamResource(new ByteArrayInputStream(binderConfiguration.getBytes()));

		Collection<BinderType> twoBinderConfigurations
				= BinderTypeRegistryAutoConfiguration.parseBinderConfigurations(classLoader, twoBinderConfigurationResource);

		Assert.assertThat(twoBinderConfigurations.size(), equalTo(2));
		Assert.assertThat(twoBinderConfigurations, containsInAnyOrder(
				both(hasProperty("defaultName", equalTo("redis"))).and(
						hasProperty("configurationClasses", hasItemInArray(String.class))),
				both(hasProperty("defaultName", equalTo("rabbit"))).and(
						hasProperty("configurationClasses", hasItemInArray(Integer.class)))
		));

	}

	@Test
	@SuppressWarnings("unchecked")
	public void testParseTwoBindersWithMultipleClasses() throws Exception {
		// this is just checking that resources are passed and classes are loaded properly
		// class values used here are not binder configurations
		String binderConfiguration = "redis=java.lang.String,java.lang.Double\n" +
				"rabbit=java.lang.Integer";
		Resource binderConfigurationResource =
				new InputStreamResource(new ByteArrayInputStream(binderConfiguration.getBytes()));

		Collection<BinderType> binderConfigurations
				= BinderTypeRegistryAutoConfiguration.parseBinderConfigurations(classLoader, binderConfigurationResource);

		Assert.assertThat(binderConfigurations.size(), equalTo(2));
		Assert.assertThat(binderConfigurations, containsInAnyOrder(
				both(hasProperty("defaultName", equalTo("redis"))).and(
						hasProperty("configurationClasses", arrayContainingInAnyOrder(String.class,Double.class))),
				both(hasProperty("defaultName", equalTo("rabbit"))).and(
						hasProperty("configurationClasses", hasItemInArray(Integer.class)))
		));

	}
}
