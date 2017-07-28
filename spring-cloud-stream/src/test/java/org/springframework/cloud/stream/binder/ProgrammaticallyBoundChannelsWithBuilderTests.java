/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.cloud.stream.binder;

import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.binding.Bindable;
import org.springframework.cloud.stream.binding.BindingsBuilder;
import org.springframework.cloud.stream.binding.BindingTargetFactory;
import org.springframework.cloud.stream.utils.MockBinderRegistryConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author Marius Bogoevici
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, classes = ProgrammaticallyBoundChannelsWithBuilderTests.TestProgrammaticBindings.class)
public class ProgrammaticallyBoundChannelsWithBuilderTests {

	@SuppressWarnings("rawtypes")
	@Autowired
	private BinderFactory binderFactory;

	@Autowired
	BindingsBuilder.ReferenceHoldingBindable referenceHoldingBindable;

	@SuppressWarnings("unchecked")
	@Test
	public void testSourceOutputChannelBound() {
		Binder binder = this.binderFactory.getBinder(null, MessageChannel.class);
		Mockito.verify(binder).bindConsumer(eq("input"), anyString(),
				eq(referenceHoldingBindable.getInput("input", MessageChannel.class)),
				Mockito.<ConsumerProperties>any());
		Mockito.verify(binder).bindProducer(eq("output"),
				eq(referenceHoldingBindable.getOutput("output", MessageChannel.class)),
				Mockito.<ProducerProperties>any());
		verifyNoMoreInteractions(binder);
	}

	@EnableBinding
	@EnableAutoConfiguration
	@Import(MockBinderRegistryConfiguration.class)
	public static class TestProgrammaticBindings {

		@Bean
		Bindable bindable(final Map<String, BindingTargetFactory> bindingTargetFactories) {
			return new BindingsBuilder(bindingTargetFactories)
					.withInput("input", MessageChannel.class)
					.withOutput("output", MessageChannel.class)
					.build();
		}

	}
}
