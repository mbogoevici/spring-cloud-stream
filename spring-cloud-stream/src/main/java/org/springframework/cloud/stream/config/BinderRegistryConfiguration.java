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

package org.springframework.cloud.stream.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.cloud.stream.binder.BinderConfiguration;
import org.springframework.cloud.stream.binder.BinderConfigurationRegistry;
import org.springframework.cloud.stream.binder.BinderTypeRegistry;
import org.springframework.cloud.stream.binder.DefaultBinderConfigurationRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.Assert;

/**
 * @author Marius Bogoevici
 */
@Configuration
public class BinderRegistryConfiguration {

	@Bean
	@ConditionalOnMissingBean(BinderConfigurationRegistry.class)
	@ConditionalOnExpression(value = "!T(org.springframework.util.CollectionUtils).isEmpty(${spring.cloud.stream.binders:null})")
	public BinderConfigurationRegistry binderConfigurationRegistry(BinderTypeRegistry binderTypeRegistry,
	                                                               ChannelBindingServiceProperties channelBindingServiceProperties) {
		Map<String, BinderConfiguration> binderConfigurations = new HashMap<>();
		for (Map.Entry<String, BinderProperties> binderEntry :
				channelBindingServiceProperties.getBinders().entrySet()) {
			if (binderTypeRegistry.get(binderEntry.getKey()) != null) {
				binderConfigurations.put(binderEntry.getKey(),
						new BinderConfiguration(binderTypeRegistry.get(binderEntry.getKey()),
								binderEntry.getValue().getProperties()));
			}
			else {
				Assert.hasText(binderEntry.getValue().getType(), "No 'type' property present for custom " +
						"binder " + binderEntry.getKey());
				binderConfigurations.put(binderEntry.getKey(),
						new BinderConfiguration(binderTypeRegistry.get(binderEntry.getValue().getType()),
								binderEntry.getValue().getProperties()));
			}
		}
		return new DefaultBinderConfigurationRegistry(binderConfigurations);
	}
}


